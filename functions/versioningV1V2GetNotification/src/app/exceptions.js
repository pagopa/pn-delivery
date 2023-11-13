class ValidationException extends Error{
    constructor(message) {
        super(message);
        this.name = "ValidationException";
    }
}

module.exports = {ValidationException};