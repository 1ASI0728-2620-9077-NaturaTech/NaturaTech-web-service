package com.plantsync.platform.rewards;

import static org.springframework.http.HttpStatus.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.*;
import org.web3j.protocol.*;
import org.web3j.protocol.core.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

@Component
public class SepoliaClient {
  private final String rpc, contract, key;
  private final java.util.function.Supplier<Web3j> connections;

  @org.springframework.beans.factory.annotation.Autowired
  public SepoliaClient(
      @Value("${integrations.sepolia.rpc:}") String rpc,
      @Value("${integrations.sepolia.contract:}") String contract,
      @Value("${integrations.sepolia.minter-key:}") String key) {
    this(rpc, contract, key, () -> Web3j.build(new HttpService(rpc)));
  }

  SepoliaClient(
      String rpc, String contract, String key, java.util.function.Supplier<Web3j> connections) {
    this.connections = connections;
    this.rpc = rpc;
    this.contract = contract;
    this.key = key;
  }

  public boolean available() {
    return rpc.startsWith("https://")
        && contract.matches("0x[0-9a-fA-F]{40}")
        && key.matches("(0x)?[0-9a-fA-F]{64}");
  }

  private Web3j connection() {
    if (!available())
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "SEPOLIA_NOT_CONFIGURED");
    return connections.get();
  }

  private void chain(Web3j web) throws Exception {
    if (!web.ethChainId().send().getChainId().equals(BigInteger.valueOf(WalletService.CHAIN)))
      throw new IllegalStateException("Wrong network");
  }

  public record Signed(String raw, String hash) {}

  public Signed prepare(String rewardId, String recipient) throws Exception {
    var web = connection();
    try {
      chain(web);
      var credentials = Credentials.create(key);
      var code = web.ethGetCode(contract, DefaultBlockParameterName.LATEST).send();
      if (code.hasError() || code.getCode() == null || code.getCode().length() <= 2)
        throw new ResponseStatusException(SERVICE_UNAVAILABLE, "BADGE_CONTRACT_MISSING");
      var nonce =
          web.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING)
              .send()
              .getTransactionCount();
      var gasPrice = web.ethGasPrice().send().getGasPrice();
      if (gasPrice.compareTo(BigInteger.valueOf(100_000_000_000L)) > 0)
        throw new ResponseStatusException(SERVICE_UNAVAILABLE, "GAS_PRICE_LIMIT");
      var fn =
          new Function(
              "mint",
              List.of(
                  new Address(recipient),
                  new Bytes32(Hash.sha3(rewardId.getBytes(StandardCharsets.UTF_8)))),
              List.of());
      var tx =
          RawTransaction.createTransaction(
              nonce,
              gasPrice,
              BigInteger.valueOf(300000),
              contract,
              BigInteger.ZERO,
              FunctionEncoder.encode(fn));
      var bytes = TransactionEncoder.signMessage(tx, WalletService.CHAIN, credentials);
      return new Signed(Numeric.toHexString(bytes), Numeric.toHexString(Hash.sha3(bytes)));
    } finally {
      web.shutdown();
    }
  }

  public String reconcile(Reward reward) throws Exception {
    var web = connection();
    try {
      chain(web);
      var receipt =
          web.ethGetTransactionReceipt(reward.getTransactionHash()).send().getTransactionReceipt();
      if (receipt.isPresent()) {
        var r = receipt.get();
        var canonical =
            web.ethGetBlockByNumber(DefaultBlockParameter.valueOf(r.getBlockNumber()), false)
                .send()
                .getBlock();
        if (canonical == null || !canonical.getHash().equals(r.getBlockHash())) return "SUBMITTED";
        var height = web.ethBlockNumber().send().getBlockNumber();
        if (height.subtract(r.getBlockNumber()).compareTo(BigInteger.valueOf(11)) < 0)
          return "SUBMITTED";
        if (!r.isStatusOK()) return "FAILED";
        String transfer = Hash.sha3String("Transfer(address,address,uint256)");
        String recipient =
            Numeric.toHexStringWithPrefixZeroPadded(
                new BigInteger(reward.getRecipient().substring(2), 16), 64);
        String token =
            Numeric.toHexString(Hash.sha3(reward.getId().getBytes(StandardCharsets.UTF_8)));
        boolean minted =
            r.getLogs().stream()
                .anyMatch(
                    log ->
                        contract.equalsIgnoreCase(log.getAddress())
                            && log.getTopics().size() == 4
                            && transfer.equals(log.getTopics().get(0))
                            && new BigInteger(log.getTopics().get(1).substring(2), 16).signum() == 0
                            && recipient.equalsIgnoreCase(log.getTopics().get(2))
                            && token.equalsIgnoreCase(log.getTopics().get(3)));
        return minted ? "CONFIRMED" : "FAILED";
      }
      // Re-broadcast exactly the durable signed transaction; never allocate another nonce here.
      var sent = web.ethSendRawTransaction(reward.getSignedTransaction()).send();
      if (!sent.hasError()
          && !reward.getTransactionHash().equalsIgnoreCase(sent.getTransactionHash()))
        throw new IllegalStateException("Unexpected transaction hash");
      return "SUBMITTED";
    } finally {
      web.shutdown();
    }
  }
}
