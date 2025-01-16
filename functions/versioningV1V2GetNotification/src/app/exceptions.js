class ValidationException extends Error{
    constructor(message) {
        super(message);
        this.name = "ValidationException";
    }
}

class DeceasedWorkflowException extends Error {
    constructor(message) {
        super(message);
        this.name = "DeceasedWorkflowException";
    }
}

module.exports = {ValidationException, DeceasedWorkflowException};