async function sendMessages(messages) {
  try {
    // anche se la configurazione per ora prevede una batch window di 10, se un domani questa dovesse ingrandirsi
    // meglio eseguire lo splice a prescindere, per essere sicuri di non sforare i 10 elementi supportati dalla SendMessageBatchCommand
    console.log(
      "Proceeding to send " + messages.length + " messages to " + QUEUE_URL
    );
    const input = {
      Entries: messages.splice(0, 10), // prendo i primi 10 e rimuovendoli dall'array originale
      QueueUrl: QUEUE_URL,
    };

    console.log("Sending batch message: %j", input);

    const command = new SendMessageBatchCommand(input);
    const response = await sqs.send(command);
    console.log("Sent message response: %j", response);
    if (response.Failed && response.Failed.length > 0) {
      console.log(
        "error sending some message totalErrors:" + response.Failed.length
      );
      throw new Error("Failed to send some messages");
    }

    if (messages.length > 0) {
      console.log("There are " + messages.length + " messages to send");
      await sendMessages(messages);
    }
  } catch (exc) {
    console.log("error sending message", exc);
    throw exc;
  }
}

module.exports = { sendMessages };
