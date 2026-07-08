const { expect } = require('chai');
const { ItemNotFoundException } = require('../app/lib/exceptions');

describe('test ItemNotFoundException', () => {
  it('should set name', () => {
    const error = new ItemNotFoundException('testKey', 'testTable');
    expect(error.name).to.equal('ItemNotFoundException');
    expect(error.message).to.include('testKey');
    expect(error.message).to.include('testTable');
  });
});
