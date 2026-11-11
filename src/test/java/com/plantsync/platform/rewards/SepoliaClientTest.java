package com.plantsync.platform.rewards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;

class SepoliaClientTest {
  Web3j web;
  SepoliaClient client;
  String contract = "0x" + "a".repeat(40), recipient = "0x" + "b".repeat(40);
  Reward reward;

  @BeforeEach
  void setup() throws Exception {
    web = mock(Web3j.class, RETURNS_DEEP_STUBS);
    client = new SepoliaClient("https://rpc.invalid", contract, "1".repeat(64), () -> web);
    when(web.ethChainId().send().getChainId()).thenReturn(BigInteger.valueOf(11155111));
    reward = new Reward();
    reward.setId("achievement-123");
    reward.setRecipient(recipient);
    reward.setTransactionHash("0x" + "c".repeat(64));
    reward.setSignedTransaction("0x0123");
  }

  @Test
  void preparesSignedSepoliaTransactionAndEnforcesGasAndContractChecks() throws Exception {
    when(web.ethGetCode(contract, DefaultBlockParameterName.LATEST).send().getCode())
        .thenReturn("0x6000");
    when(web.ethGetTransactionCount(anyString(), eq(DefaultBlockParameterName.PENDING))
            .send()
            .getTransactionCount())
        .thenReturn(BigInteger.valueOf(7));
    when(web.ethGasPrice().send().getGasPrice()).thenReturn(BigInteger.valueOf(1000000000));
    var signed = client.prepare(reward.getId(), recipient);
    var decoded = TransactionDecoder.decode(signed.raw());
    assertEquals(BigInteger.valueOf(7), decoded.getNonce());
    assertEquals(contract, decoded.getTo());
    assertEquals(
        Numeric.toHexString(Hash.sha3(Numeric.hexStringToByteArray(signed.raw()))), signed.hash());
    verify(web).shutdown();
    when(web.ethGasPrice().send().getGasPrice()).thenReturn(BigInteger.valueOf(100000000001L));
    assertEquals(
        "GAS_PRICE_LIMIT",
        assertThrows(ResponseStatusException.class, () -> client.prepare("id", recipient))
            .getReason());
    when(web.ethGetCode(contract, DefaultBlockParameterName.LATEST).send().getCode())
        .thenReturn("0x");
    assertEquals(
        "BADGE_CONTRACT_MISSING",
        assertThrows(ResponseStatusException.class, () -> client.prepare("id", recipient))
            .getReason());
  }

  @Test
  void rejectsWrongNetworkAndMissingConfiguration() throws Exception {
    assertFalse(new SepoliaClient("http://rpc.invalid", contract, "1".repeat(64)).available());
    assertThrows(
        ResponseStatusException.class,
        () -> new SepoliaClient("", "", "").prepare("id", recipient));
    when(web.ethChainId().send().getChainId()).thenReturn(BigInteger.ONE);
    assertThrows(IllegalStateException.class, () -> client.prepare("id", recipient));
    verify(web).shutdown();
  }

  TransactionReceipt receipt() throws Exception {
    var receipt = new TransactionReceipt();
    receipt.setBlockNumber("0x64");
    receipt.setBlockHash("canonical");
    receipt.setStatus("0x1");
    when(web.ethGetTransactionReceipt(reward.getTransactionHash()).send().getTransactionReceipt())
        .thenReturn(Optional.of(receipt));
    when(web.ethGetBlockByNumber(any(), eq(false)).send().getBlock().getHash())
        .thenReturn("canonical");
    when(web.ethBlockNumber().send().getBlockNumber()).thenReturn(BigInteger.valueOf(111));
    return receipt;
  }

  @Test
  void requiresCanonicalBlockAndTwelveConfirmations() throws Exception {
    var receipt = receipt();
    receipt.setBlockHash("orphan");
    assertEquals("SUBMITTED", client.reconcile(reward));
    receipt.setBlockHash("canonical");
    when(web.ethBlockNumber().send().getBlockNumber()).thenReturn(BigInteger.valueOf(110));
    assertEquals("SUBMITTED", client.reconcile(reward));
    when(web.ethBlockNumber().send().getBlockNumber()).thenReturn(BigInteger.valueOf(111));
    receipt.setStatus("0x0");
    assertEquals("FAILED", client.reconcile(reward));
  }

  @Test
  void requiresMatchingMintTransferRatherThanOnlySuccessfulReceipt() throws Exception {
    var receipt = receipt();
    receipt.setLogs(List.of());
    assertEquals("FAILED", client.reconcile(reward));
    var log = new Log();
    log.setAddress(contract);
    log.setTopics(
        List.of(
            Hash.sha3String("Transfer(address,address,uint256)"),
            "0x" + "0".repeat(64),
            Numeric.toHexStringWithPrefixZeroPadded(new BigInteger(recipient.substring(2), 16), 64),
            Numeric.toHexString(Hash.sha3(reward.getId().getBytes(StandardCharsets.UTF_8)))));
    receipt.setLogs(List.of(log));
    assertEquals("CONFIRMED", client.reconcile(reward));
    log.setAddress(recipient);
    assertEquals("FAILED", client.reconcile(reward));
  }

  @Test
  void rebroadcastsOnlyDurableTransactionAndRejectsUnexpectedHash() throws Exception {
    when(web.ethGetTransactionReceipt(reward.getTransactionHash()).send().getTransactionReceipt())
        .thenReturn(Optional.empty());
    when(web.ethSendRawTransaction("0x0123").send().getTransactionHash())
        .thenReturn(reward.getTransactionHash());
    clearInvocations(web);
    assertEquals("SUBMITTED", client.reconcile(reward));
    verify(web).ethSendRawTransaction("0x0123");
    when(web.ethSendRawTransaction("0x0123").send().getTransactionHash()).thenReturn("other");
    assertThrows(IllegalStateException.class, () -> client.reconcile(reward));
  }
}
