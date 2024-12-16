function headerMapper(event) {
    const headers = {
        ...event.headers,
        "x-pagopa-pn-src-ch": "B2B",
    };
    
    const authorizer = event.requestContext.authorizer;

    const headerMappings = {
        "cx_groups": "x-pagopa-pn-cx-groups",
        "cx_id": "x-pagopa-pn-cx-id",
        "cx_role": "x-pagopa-pn-cx-role",
        "cx_type": "x-pagopa-pn-cx-type",
        "cx_jti": "x-pagopa-pn-jti",
        "sourceChannelDetails": "x-pagopa-pn-src-ch-details",
        "uid": "x-pagopa-pn-uid",
    };

    Object.entries(headerMappings).forEach(([key, headerName]) => {
        if (authorizer[key]) {
        headers[headerName] = authorizer[key];
        }
    });

    return headers;
};
  
module.exports = { headerMapper };