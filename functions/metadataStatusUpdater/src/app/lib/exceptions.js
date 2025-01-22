class ItemNotFoundException extends Error {
  constructor(key, tableName) {
    super(`Item with id = ${key} not found on table ${tableName}`);
    this.name = "ItemNotFoundException";
  }
}

module.exports = { ItemNotFoundException };
