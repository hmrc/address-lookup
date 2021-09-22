# Deprecated Endpoints

The following endpoints are deprecated and the process to decommission them has started.  If you are creating a new integration with Address Lookup you should not use these endpoints! 

This information is for services that have previously integrated with us, and have not yet moved across to our POST endpoints.

## 1. UK Address Search

Two methods are provided for searching for addresses. In both cases, a list of zero or more
addresses is returned. The response format is a _JSON array_ containing *zero or more*
[UK Address Objects (v2)](uk-address-object.json). [Example response (v2)](example-response-multiple.json). 

If an address is a PO Box the response will contain an optional attribute named `poBox`, see the [PO Box example response](example-response-pobox.json).

### 1A. (GET) Lookup by UPRN (<span style="color: red">DEPRECATED</span>)

This is a simple query endpoint that searches for the address(es) of a given UPRN.

URL:

- `{contextPath}/v2/uk/addresses`
- `{contextPath}/v2/gb/addresses` (alias for `uk`)

Example URL:

- `{contextPath}/v2/uk/addresses?uprn=1234512345`

Methods:

- `GET` - __*<span style="color: red">DEPRECATED</span>* Please migrated to the `POST` version__

Headers:

- `User-Agent` (required): *string*

  This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
  string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
  problems with default User-Agent values.
  It will be used for reporting. All requests from a given origin must carry the same code-name.

- `X-Hmrc-Origin` (alternative): *string*

  The is an alternative to `User-agent`; only one of these is required.

- `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

  Example:

    - `Accept-Language: cy, en`

  If no match was made, the default response will be sent, which will typically be English.

  Note that clients of this service that have user-facing UIs may pass the Accept-Language
  header sent by the user-agent through directly.

Query params:

- `uprn` (required).

Status codes:

- *200-OK* when the postcode search was successful (n.b. response might be `[]`)
- others as in section 1.

Response:

- `Content-Type: application/json`
- Expiry and cache control headers will be set appropriately.
- The body will be a _JSON array_ containing *zero or more* [UK Address Objects (v2)](uk-address-object.json).
  [Example response (v2)](example-response-multiple.json)

### 1B. (GET) Lookup by Postcode (<span style="color: red">DEPRECATED</span>)

This is a simple query endpoint that searches for addresses at a given postcode.

URL:

- `{contextPath}/v2/uk/addresses`
- `{contextPath}/v2/gb/addresses` (alias for `uk`)

Example URL:

- `{contextPath}/v2/uk/addresses?postcode=AA1+1ZZ&filter=The+Rectory`

Methods:

- `GET` - __*<span style="color: red">DEPRECATED</span>* Please migrated to the `POST` version__

Headers:

- `User-Agent` (required): *string*

  This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
  string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
  problems with default User-Agent values.
  It will be used for reporting. All requests from a given origin must carry the same code-name.

- `X-Hmrc-Origin` (alternative): *string*

  The is an alternative to `User-agent`; only one of these is required.

- `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

  Example:

    - `Accept-Language: cy, en`

  If no match was made, the default response will be sent, which will typically be English.

  Note that clients of this service that have user-facing UIs may pass the Accept-Language
  header sent by the user-agent through directly.

Query params:

- `postcode` (required) in the usual Royal Mail format, all uppercase. The internal space may be omitted.
- `filter` (optional): a sub-string match on any of the address lines.

Status codes:

- *200-OK* when the postcode search was successful (n.b. response might be `[]`)
- others as in section 1.

Response:

- `Content-Type: application/json`
- Expiry and cache control headers will be set appropriately.
- The body will be a _JSON array_ containing *zero or more* [UK Address Objects (v2)](uk-address-object.json).
  [Example response (v2)](example-response-multiple.json)

### 1C. (GET) Lookup by Outcode (<span style="color: red">DEPRECATED</span>)

This is a simple query endpoint that searches for addresses within a given outcode (the first half of a postcode).

URL:

- `{contextPath}/v2/uk/addresses`
- `{contextPath}/v2/gb/addresses` (alias for `uk`)

Example URL:

- `{contextPath}/v2/uk/addresses?outcode=AA1+1ZZ&filter=The+Rectory`

Methods:

- `GET` __*<span style="color: red">DEPRECATED</span>* Please migrated to the `POST` version__

Headers:

- `User-Agent` (required): *string*

  This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
  string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
  problems with default User-Agent values.
  It will be used for reporting. All requests from a given origin must carry the same code-name.

- `X-Hmrc-Origin` (alternative): *string*

  The is an alternative to `User-agent`; only one of these is required.

- `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

  Example:

    - `Accept-Language: cy, en`

  If no match was made, the default response will be sent, which will typically be English.

  Note that clients of this service that have user-facing UIs may pass the Accept-Language
  header sent by the user-agent through directly.

Query params:

- `outcode` (required) the first half of a postcode, all uppercase.
- `filter` (required): a sub-string match on any of the address lines.

Status codes:

- *200-OK* when the outcode search was successful (n.b. response might be `[]`)
- others as in section 1.

Response:

- `Content-Type: application/json`
- Expiry and cache control headers will be set appropriately.
- The body will be a _JSON array_ containing *zero or more* [UK Address Objects (v2)](uk-address-object.json).
  [Example response (v2)](example-response-multiple.json)

### 1D. (GET) Arbitrary Address Match (<span style="color: red">DEPRECATED</span>)

This is a more advanced query endpoint that takes an arbitrary address and searches for the best match or matches.
The canonical address(es) is returned.

URL:

- `{contextPath}/v2/uk/addresses`
- `{contextPath}/v2/gb/addresses` (alias for `uk`)

Example URL:

- `{contextPath}/v2/uk/addresses?postcode=AA1+1ZZ&line1=The+Rectory&line2=Church+Street&town=Townham`

Methods:

- `GET` - __*<span style="color: red">DEPRECATED</span>* Please migrated to the `POST` version__

Headers:

- `User-Agent` (required): *string*

  This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
  string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
  problems with default User-Agent values.
  It will be used for reporting. All requests from a given origin must carry the same code-name.

- `X-Hmrc-Origin` (alternative): *string*

  The is an alternative to `User-agent`; only one of these is required.

- `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

  Example:

    - `Accept-Language: cy, en`

  If no match was made, the default response will be sent, which will typically be English.

  Note that clients of this service that have user-facing UIs may pass the Accept-Language
  header sent by the user-agent through directly.

Query params:

- `line1` (optional): the first line of the input address
- `line2` (optional): the second line of the input address
- `line3` (optional): the third line of the input address
- `line4` (optional): the fourth line of the input address
- `town` (optional): the name of the town in the input address, if not included in lines 1-4
- `postcode` (optional) in the usual Royal Mail format, all uppercase. The internal space may be omitted.
- `limit` (optional) a positive integer that constrains the number of matching addresses found (this reduces server load considerably).

Status codes:

- *200-OK* when the search was successful (n.b. response might be `[]`)
- others as in section 1.

Response:

- `Content-Type: application/json`
- Expiry and cache control headers will be set appropriately.
- The body will be a _JSON array_ containing *zero or more* [UK Address Objects (v2)](uk-address-object.json).
  [Example response (v2)](example-response-multiple.json)

## 2. BFPO Address Lookup By Postcode or BFPO Number (<span style="color: red">DEPRECATED</span>)

This endpoint is deprecated, BFPO addresses can be found using the standard [Lookup by Postcode](address-lookup-api.md#4B.-(POST)-Lookup-by-Postcode) endpoint

URL:

- `{contextPath}/bfpo/addresses`

Example URLs:

- `{contextPath}/bfpo/addresses?bfpo=105&filter=2014`

Methods:

- `GET`

Headers:

- `User-Agent` (required): *string*

  This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
  string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
  problems with default User-Agent values.
  It will be used for reporting. All requests from a given origin must carry the same code-name.

Query params:

- `postcode` (required): the value must match the regex shown in the address schema above.
- `bfpo` (alternative to postcode): a BFPO number; these normally don't contain non-numeric characters, but there
  are some unusual cases that do.
- `filter` (optional): a sub-string match on any of the address lines.

Status codes:

- *200-OK* when the BFPO search was successful (n.b. response might be `[]`)
- others as in section 1.

Response:

- `Content-Type: application/json`
- Expiry and cache control headers will be set appropriately.
- The body will be a JSON array containing *zero or more* BFPO Address Response Objects.
  [Example response](bfpo-response-sample1.json).

The response format can be represented as a Scala case-class thus:

```
case class BFPO(operation: Option[String], lines: List[String], postcode: String, bfpoNo: String)
```
