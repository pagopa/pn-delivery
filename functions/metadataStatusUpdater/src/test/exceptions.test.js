const {ItemNotFoundException} = require("../app/lib/exceptions.js");
const { expect } = require("chai");

describe("test ItemNotFoundException", () => {
    it("should set name", () => {
      const testKey = "testKey";
      const testTable = "testTable";
      const exception = new ItemNotFoundException(testKey, testTable);
      expect(exception.name).to.eq("ItemNotFoundException");
      expect(exception.message).to.eq(
        `Item with id = ${testKey} not found on table ${testTable}`
      );
    });
  });
   