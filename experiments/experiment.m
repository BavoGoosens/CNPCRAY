[tasksWindow1Standard, tasksWindow2Standard, tasksWindow3Standard, tasksWindow4Standard, tasksHopsStandard, agentsCountsStandard, agentsMessagesStandard, batteryStationsStandard, taskStationsStandard] = analyseCNP('standard');
[tasksWindow1LG, tasksWindow2LG, tasksWindow3LG, tasksWindow4LG, tasksHopsLG, agentsCountsLG, agentsMessagesLG, batteryStationsLG, taskStationsLG] = analyseCNP('less greedy');
[tasksWindow1LGF, tasksWindow2LGF, tasksWindow3LGF, tasksWindow4LGF, tasksHopsLGF, agentsCountsLGF, agentsMessagesLGF, batteryStationsLGF, taskStationsLGF] = analyseCNP('less greedy fixed');
[tasksWindow1Prop, tasksWindow2Prop, tasksWindow3Prop, tasksWindow4Prop, tasksHopsProp, agentsCountsProp, agentsMessagesProp, batteryStationsProp, taskStationsProp] = analyseCNP('propagate');

f = figure('visible', 'off');
plot(tasksWindow1Standard);
hold on;
title('Tasks: window 1 (task arrived - task manager found)');
plot(tasksWindow1LG);
plot(tasksWindow1LGF);
plot(tasksWindow1Prop);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'window1', 'png');

f = figure('visible', 'off');
plot(tasksWindow2Standard);
hold on;
title('Tasks: window 2 (task manager found - pick up)');
plot(tasksWindow2LG);
plot(tasksWindow2LGF);
plot(tasksWindow2Prop);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'window2', 'png');

f = figure('visible', 'off');
plot(tasksWindow3Standard);
hold on;
title('Tasks: window 3 (pick up - delivery)');
plot(tasksWindow3LG);
plot(tasksWindow3LGF);
plot(tasksWindow3Prop);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off
saveas(f, 'window3', 'png');

f = figure('visible', 'off');
plot(tasksWindow4Standard);
hold on;
title('Tasks: window 4 (task arrived - delivery)');
plot(tasksWindow4LG);
plot(tasksWindow4LGF);
plot(tasksWindow4Prop);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'window4', 'png');

f = figure('visible', 'off');
plot(tasksHopsStandard);
hold on;
title('Tasks: number of hops');
plot(tasksHopsLG);
plot(tasksHopsLGF);
plot(tasksHopsProp);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'hops', 'png');

f = figure('visible', 'off');
plot(agentsCountsStandard);
hold on;
title('Agents: number of counts');
plot(agentsCountsLG);
plot(agentsCountsLGF);
plot(agentsCountsProp);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'counts', 'png');

f = figure('visible', 'off');
plot(agentsMessagesStandard);
hold on;
title('Agents: number of messages');
plot(agentsMessagesLG);
plot(agentsMessagesLGF);
plot(agentsMessagesProp);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'agents_messages', 'png');

f = figure('visible', 'off');
plot(batteryStationsStandard);
hold on;
title('Battery stations: energy loaded');
plot(batteryStationsLG);
plot(batteryStationsLGF);
plot(batteryStationsProp);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'energy_loaded', 'png');

f = figure('visible', 'off');
plot(taskStationsStandard);
hold on;
title('Task stations: number of messages');
plot(taskStationsLG);
plot(taskStationsLGF);
plot(taskStationsProp);
legend('Standard', 'Less greedy', 'Less greedy fixed', 'Propagate');
hold off;
saveas(f, 'task_station_messages', 'png');