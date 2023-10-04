const { createNewNotificationRequesV21 } = require('../app/requestHelper')
const fs = require("fs");
const { expect } = require("chai");

describe('RequestHelper Testing', () => {
    describe('createNewNotificationRequesV21 Testing', () => {
        it('should return newNotificationRequestV21', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let newNotificationRequestV1 = JSON.parse(newNotificationRequestV21JSON);
            const newNotificationRequestV21 = createNewNotificationRequesV21(newNotificationRequestV1);
            expect(newNotificationRequestV21).to.not.be.null;
            expect(newNotificationRequestV21).to.not.be.undefined; 
        });

        it('should return newNotificationRequestV21 without payments', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let newNotificationRequestV1 = JSON.parse(newNotificationRequestV21JSON);
            newNotificationRequestV1.recipients[0].payment = null;
            const newNotificationRequestV21 = createNewNotificationRequesV21(newNotificationRequestV1);
            expect(newNotificationRequestV21).to.not.be.null;
            expect(newNotificationRequestV21).to.not.be.undefined; 
        });
    });
});