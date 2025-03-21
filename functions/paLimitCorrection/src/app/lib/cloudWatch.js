const { CloudWatchClient, GetMetricStatisticsCommand } = require("@aws-sdk/client-cloudwatch");

async function getIteratorAgeMetrics(lambdaName) {
    console.log(`Fetching IteratorAge metrics for ${lambdaName}`);

    const client = new CloudWatchClient();
    const endTime = new Date();
    const startTime = new Date(endTime.getTime() - 10 * 60 * 1000); // Ultimi 10 minuti
    console.log('Start time:', startTime.toISOString());
    console.log('End time:', endTime.toISOString());

    const input = {
        Namespace: 'AWS/Lambda',
        MetricName: 'IteratorAge',
        Dimensions: [
            {
                Name: 'FunctionName',
                Value: lambdaName
            }
        ],
        StartTime: startTime,
        EndTime: endTime,
        Period: 60,
        Statistics: ['Maximum'],
        Unit: 'Milliseconds'
    };

    try {
        const command = new GetMetricStatisticsCommand(input);
        const response = await client.send(command);
        console.log('Metrics fetched:', JSON.stringify(response.Datapoints));
        return response.Datapoints;
    } catch (error) {
        console.error('Error fetching IteratorAge metrics:', error);
        throw error;
    }
}

module.exports = { getIteratorAgeMetrics };