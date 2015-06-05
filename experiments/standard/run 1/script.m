files = dir('*.csv');
for file = files'
    csv = csvread(file.name);
    % Do some stuff
    csv;
end