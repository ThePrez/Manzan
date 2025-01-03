const { SMTPServer } = require('smtp-server');

// Create a new SMTP server instance
const server = new SMTPServer({
    secure: false,
    disabledCommands: ['AUTH'],
    onData(stream, session, callback) {
        let emailData = '';

        // Collect the email data
        stream.on('data', chunk => {
            emailData += chunk.toString();
        });

        stream.on('end', () => {
            console.log('Received email:');
            console.log(emailData);
            callback(); // Signal that the email has been processed
        });
    }
});

// Start the SMTP server
server.listen(8019, () => {
    console.log('SMTP server is running on port 8019');
});
