-- Support owner-scoped history, worker queues and continuity scans.
create index ix_analyses_owner_created on plant_analyses (owner_id, created_at);
create index ix_analyses_status_created on plant_analyses (status, created_at);
create index ix_conversations_owner on agent_conversations (owner_id);
create index ix_proposals_node_status on agent_action_proposals (node_code, command_status);
create index ix_proposals_node_confirmed on agent_action_proposals (node_id, confirmed_at);
create index ix_readings_node_timestamp on sensor_readings (node_id, timestamp);
create index ix_rewards_status_started on green_rewards (status, started_at);
create index ix_profiles_user on profiles (user_id);
create index ix_plants_profile on plants (profile_id);
create index ix_nodes_profile on iot_nodes (profile_id);
create index ix_nodes_plant on iot_nodes (plant_id);
create index ix_tasks_plant on tasks (plant_id);
