const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV25ToV24 = function(responseV25) {
    console.log("transformFromV25ToV24");

    // eliminazione additionalLanguages per tutte le api con versione precedente all’ultima
    let responseV24 = responseV25;
    responseV24.additionalLanguages = undefined;
    
    return responseV24;
}