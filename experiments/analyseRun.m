function [tasks, agents, batteryStations, taskStations] = analyseRun(cnpFolder, runFolder)
    
    % go to the right folder
    cd(cnpFolder);
    cd(runFolder);
    
    % read files in folder
    files = dir('*csv');
    
    % initialize variables
    taskCount = 1;
    tasks = zeros(400, 5);
    
    agentCount = 1;
    agents = zeros(50,2);
    
    batteryStationCount = 1;
    batteryStations = zeros(4,1);
    
    taskStationCount = 1;
    taskStations = zeros(4,1);
    
    for file = files'
        fid = fopen(file.name);
        taskFile = length(strfind(file.name, 'Task@')) == 1;
        agentFile = length(strfind(file.name, 'Agent')) == 1;
        batteryStationFile = length(strfind(file.name, 'Battery station')) == 1;
        taskStationFile = length(strfind(file.name, 'Task station')) == 1;
        if (taskFile)
            taskCell = textscan(fid, '%s%s%d64', 'delimiter', ',;:');
            map = containers.Map(taskCell{1,2}, taskCell{1,3});
            try
                window1 = abs(map('time_of_creation ') - map('initial taskmanager found '));
            catch
                window1 = NaN;
            end
            try
                window2 = abs(map('initial taskmanager found ') - map('pick up '));
            catch
                window2 = NaN;
            end
            try
                window3 = abs(map('pick up ') - map('deliver '));
            catch
                window3 = NaN;
            end
            try
                window4 = abs(map('time_of_creation ') - map('deliver '));
            catch
                window4 = NaN;
            end
            try
                hops = map('hops ');
            catch
                hops = 0;
            end
            tasks(taskCount, 1) = window1;
            tasks(taskCount, 2) = window2;
            tasks(taskCount, 3) = window3;
            tasks(taskCount, 4) = window4;
            tasks(taskCount, 5) = hops;
            taskCount = taskCount + 1;
        end
        if (agentFile)
            counts = 0;
            messages = 0;
            taskCell = textscan(fid, '%s%s%d64', 'delimiter', ',;:');
            types = taskCell{1,1};
            for typeMessage = types'
                type = typeMessage{1};
                if (strcmp(type, 'count '))
                    counts = counts + 1;
                end
                if (strcmp(type, 'communication '))
                    messages = messages + 1;
                end
            end
            agents(agentCount, 1) = counts;
            agents(agentCount, 2) = messages;
            agentCount = agentCount + 1;
        end
        if (batteryStationFile)
            taskCell = textscan(fid, '%s%s%d64', 'delimiter', ',;:');
            energyLoaded = taskCell{1,3};
            energyLoaded = sum(energyLoaded);
            batteryStations(batteryStationCount) = energyLoaded;
            batteryStationCount = batteryStationCount + 1;
        end
        if (taskStationFile)
            taskCell = textscan(fid, '%s%s%d64', 'delimiter', ',;:');
            broadcastMessages = taskCell{1,3};
            broadcastMessages = length(broadcastMessages);
            taskStations(taskStationCount) = broadcastMessages;
            taskStationCount = taskStationCount + 1;
        end
        fclose(fid);
    end
    cd('..');
    cd('..');
end
