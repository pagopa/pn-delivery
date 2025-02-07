const { expect } = require('chai');
const proxyquire = require("proxyquire").noPreserveCache();

describe('index tests', function() {

    it("test Ok", async () => {
        const event = {}

        const lambda = proxyquire.noCallThru().load("../../index.js", {
            "./src/app/handler.js": {
                eventHandler : async () => {
                    return new Promise(res => res({
                        batchItemFailures: []
                    }))
                }
            },
        });

        const res = await lambda.handler(event, null);
        expect(res).deep.equals({
            batchItemFailures: []
        })
    });
});