function [tasksWindow1Average, tasksWindow2Average, tasksWindow3Average, tasksWindow4Average, tasksHopsAverage, agentsCountsAverage, agentsMessagesAverage, batteryStationsAverage, taskStationsAverage] = analyseCNP(cnpFolder)
    [tasksRun1, agentsRun1, batteryStationsRun1, taskStationsRun1] = analyseRun(cnpFolder, 'run 1');
    [tasksRun2, agentsRun2, batteryStationsRun2, taskStationsRun2] = analyseRun(cnpFolder, 'run 2');
    [tasksRun3, agentsRun3, batteryStationsRun3, taskStationsRun3] = analyseRun(cnpFolder, 'run 3');

    tasksWindow1Run1 = tasksRun1(:,1);
    tasksWindow1Run2 = tasksRun2(:,1);
    tasksWindow1Run3 = tasksRun3(:,1);
    tasksWindow1Average = zeros(400,1);
    
    for i = 1:400
        tasksWindow1Average(i) = (tasksWindow1Run1(i) + tasksWindow1Run2(i) + tasksWindow1Run3(i))/3;
    end
    
    tasksWindow2Run1 = tasksRun1(:,2);
    tasksWindow2Run2 = tasksRun2(:,2);
    tasksWindow2Run3 = tasksRun3(:,2);
    tasksWindow2Average = zeros(400,1);
    
    for i = 1:400
        tasksWindow2Average(i) = (tasksWindow2Run1(i) + tasksWindow2Run2(i) + tasksWindow2Run3(i))/3;
    end
    
    tasksWindow3Run1 = tasksRun1(:,3);
    tasksWindow3Run2 = tasksRun2(:,3);
    tasksWindow3Run3 = tasksRun3(:,3);
    tasksWindow3Average = zeros(400,1);
    
    for i = 1:400
        tasksWindow3Average(i) = (tasksWindow3Run1(i) + tasksWindow3Run2(i) + tasksWindow3Run3(i))/3;
    end
    
    tasksWindow4Run1 = tasksRun1(:,4);
    tasksWindow4Run2 = tasksRun2(:,4);
    tasksWindow4Run3 = tasksRun3(:,4);
    tasksWindow4Average = zeros(400,1);
    
    for i = 1:400
        tasksWindow4Average(i) = (tasksWindow4Run1(i) + tasksWindow4Run2(i) + tasksWindow4Run3(i))/3;
    end
    
    tasksHopsRun1 = tasksRun1(:,5);
    tasksHopsRun2 = tasksRun2(:,5);
    tasksHopsRun3 = tasksRun3(:,5);
    tasksHopsAverage = zeros(400,1);
    
    for i = 1:400
        tasksHopsAverage(i) = (tasksHopsRun1(i) + tasksHopsRun2(i) + tasksHopsRun3(i))/3;
    end
    
    tasksWindow1Average = sort(tasksWindow1Average);
    tasksWindow2Average = sort(tasksWindow2Average);
    tasksWindow3Average = sort(tasksWindow3Average);
    tasksWindow4Average = sort(tasksWindow4Average);
    tasksHopsAverage = sort(tasksHopsAverage);
    
    agentsCountsRun1 = agentsRun1(:,1);
    agentsCountsRun2 = agentsRun2(:,1);
    agentsCountsRun3 = agentsRun3(:,1);
    agentsCountsAverage = zeros(50, 1);
    
    for i = 1:50
        agentsCountsAverage(i) = (agentsCountsRun1(i) + agentsCountsRun2(i) + agentsCountsRun3(i)) / 3;
    end
    
    agentsMessagesRun1 = agentsRun1(:,2);
    agentsMessagesRun2 = agentsRun2(:,2);
    agentsMessagesRun3 = agentsRun3(:,2);
    agentsMessagesAverage = zeros(50, 1);
    
    for i = 1:50
        agentsMessagesAverage(i) = (agentsMessagesRun1(i) + agentsMessagesRun2(i) + agentsMessagesRun3(i)) / 3;
    end
    
    agentsCountsAverage = sort(agentsCountsAverage);
    agentsMessagesAverage = sort(agentsMessagesAverage);
    
    batteryStationsAverage = zeros(4,1);
    
    for i = 1:4
        batteryStationsAverage(i) = (batteryStationsRun1(i) + batteryStationsRun2(i) + batteryStationsRun3(i)) / 3;
    end
    
    batteryStationsAverage = sort(batteryStationsAverage);
    
    taskStationsAverage = zeros(4,1);
    
    for i = 1:4
        taskStationsAverage(i) = (taskStationsRun1(i) + taskStationsRun2(i) + taskStationsRun3(i)) / 3;
    end
    
    taskStationsAverage = sort(taskStationsAverage);
    
end
