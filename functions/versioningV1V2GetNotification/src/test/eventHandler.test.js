const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

// event handler tests
describe("event handler tests", function () {
  // a test that goes ok
  it("fake test", async function () {
    expect(Number("1")).equal(1);
  });
});
