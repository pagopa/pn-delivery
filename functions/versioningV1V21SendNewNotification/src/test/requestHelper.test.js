const { fromNewNotificationRequestV1ToV21, fromNewNotificationRequestV21ToV23, validateNewNotification } = require('../app/requestHelper')
const fs = require("fs");
const { expect } = require("chai");

describe('RequestHelper Testing', () => {
    describe('createNewNotificationRequesV21 Testing', () => {
        it('should return newNotificationRequestV21', async () => {
            const newNotificationRequestV1JSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let newNotificationRequestV1 = JSON.parse(newNotificationRequestV1JSON);
            const newNotificationRequestV21 = fromNewNotificationRequestV1ToV21(newNotificationRequestV1);
            expect(newNotificationRequestV21).to.not.be.null;
            expect(newNotificationRequestV21).to.not.be.undefined; 
        });

        it('should return newNotificationRequestV21 without payments', async () => {
            const newNotificationRequestV1JSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let newNotificationRequestV1 = JSON.parse(newNotificationRequestV1JSON);
            newNotificationRequestV1.recipients[0].payment = null;
            const newNotificationRequestV21 = fromNewNotificationRequestV1ToV21(newNotificationRequestV1);
            expect(newNotificationRequestV21).to.not.be.null;
            expect(newNotificationRequestV21).to.not.be.undefined; 
        });
    });
    describe('createNewNotificationRequestV23 Testing', () => {
        it('should return newNotificationRequestV23 default value', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let newNotificationRequestV21 = JSON.parse(newNotificationRequestV21JSON);
            const newNotificationRequestV23 = fromNewNotificationRequestV21ToV23(newNotificationRequestV21);
            expect(newNotificationRequestV23).to.not.be.null;
            expect(newNotificationRequestV23).to.not.be.undefined; 
            expect(newNotificationRequestV23.paFee).to.be.equal(100);
            expect(newNotificationRequestV23.vat).to.be.equal(22);
        })

        it('should return newNotificationRequestV23', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let newNotificationRequestV21 = JSON.parse(newNotificationRequestV21JSON);
            newNotificationRequestV21.paFee = 200;
            newNotificationRequestV21.vat = 17;
            const newNotificationRequestV23 = fromNewNotificationRequestV21ToV23(newNotificationRequestV21);
            expect(newNotificationRequestV23).to.not.be.null;
            expect(newNotificationRequestV23).to.not.be.undefined; 
            expect(newNotificationRequestV23.paFee).to.be.equal(200);
            expect(newNotificationRequestV23.vat).to.be.equal(17);
        })
    });
    describe('Validate new notification request Testing', () => {
        it('shoud return validation KO for newNotificationRequestV1 (when there is not physicalAddress)', async () => {
            const newNotificationRequestV1JSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let newNotificationRequestV1 = JSON.parse(newNotificationRequestV1JSON);
            newNotificationRequestV1.recipients[0].physicalAddress = null;
            const errors = validateNewNotification(newNotificationRequestV1, 10);
            expect(errors).to.be.an( "array" ).that.is.not.empty
            expect(errors).to.be.eql(["Validation errors: [object has missing required properties ([\"physicalAddress\"])]"]);
        });
        it('shoud return validation ok for newNotificationRequestV21', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let newNotificationRequestV21 = JSON.parse(newNotificationRequestV21JSON);
            const errors = validateNewNotification(newNotificationRequestV21, 21);
            expect(errors).to.be.an( "array" ).that.is.empty
        });
        it('shoud return validation KO for newNotificationRequestV21', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let newNotificationRequestV21 = JSON.parse(newNotificationRequestV21JSON);
            const f24Payment = {
                f24: {
                    title: "f24Test",
                    applyCost: true,
                    metadataAttachment: {}
                } 
            }
            newNotificationRequestV21.recipients[0].payments.push(f24Payment);
            const errors = validateNewNotification(newNotificationRequestV21, 21);
            expect(errors).to.be.an( "array" ).that.is.not.empty
        });
        it('shoud return validation KO for newNotificationRequestV21 (when there is not physicalAddress)', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let newNotificationRequestV21 = JSON.parse(newNotificationRequestV21JSON);
            newNotificationRequestV21.recipients[0].physicalAddress = null;
            const errors = validateNewNotification(newNotificationRequestV21, 21);
            expect(errors).to.be.an( "array" ).that.is.not.empty
            expect(errors).to.be.eql(["Validation errors: [object has missing required properties ([\"physicalAddress\"])]"]);
        });
        it('shoud return validation KO for newNotificationRequestV21 ASYNC', async () => {
            const newNotificationRequestV21JSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let newNotificationRequestV21 = JSON.parse(newNotificationRequestV21JSON);
            newNotificationRequestV21.pagoPaIntMode = 'ASYNC';
            const errors = validateNewNotification(newNotificationRequestV21, 21);
            expect(errors).to.be.an( "array" ).that.is.not.empty
        });
        it('shoud return validation OK for newNotificationRequestV24', async () => {
            const newNotificationRequestV24JSON = fs.readFileSync("./src/test/newNotificationRequestV24.json");
            let newNotificationRequestV24 = JSON.parse(newNotificationRequestV24JSON);
            const errors = validateNewNotification(newNotificationRequestV24, 24);
            expect(errors).to.be.an( "array" ).that.is.empty
        });
        it('shoud return validation KO for newNotificationRequestV24', async () => {
            const newNotificationRequestV24JSON = fs.readFileSync("./src/test/newNotificationRequestV24.json");
            let newNotificationRequestV24 = JSON.parse(newNotificationRequestV24JSON);
            newNotificationRequestV24.recipients[0].physicalAddress = null;
            const errors = validateNewNotification(newNotificationRequestV24, 24);
            expect(errors).to.be.an( "array" ).that.is.not.empty
            expect(errors).to.be.eql(["Validation errors: [object has missing required properties ([\"physicalAddress\"])]"]);
        });
    })
});