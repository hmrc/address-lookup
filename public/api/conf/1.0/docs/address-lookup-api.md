Address Lookup Microservice API
===============================

The address lookup microservice is one of HMRC's proofing and validation services that assist with cleansing
user input data. This can greatly reduce cost downstream that might otherwise arise from data inconsistencies
and ambiguities.

If you're a UK Government Organisation wanting additional information on this service, please email [txm-attribute-validation-g@digital.hmrc.gov.uk](mailto:txm-attribute-validation-g@digital.hmrc.gov.uk).

## 1. UK Address JSON Object

To avoid later repetition, the common format of each UK address returned in responses from the
service is described first. The actual REST responses *contain* these documents in arrays, and
are described elsewhere.

Please note that the string `maxLength` values in the schema are preliminary and
**must not** be used for designing database schemas etc.

See [the UK address schema (v2)](uk-address-object.json) and address [example response](example-response.json).  
Suitable Scala case classes can be derived from
[address-lookup-frontend](https://github.com/hmrc/address-lookup-frontend/tree/master/app/address/v2).

## 2. General Error Conditions

Status codes as per [RFC-7231 section 6](https://tools.ietf.org/html/rfc7231#section-6):

 - *400-Bad request*: when input parameters are not valid, which is when
    - a required header is absent,
    - a required parameter is absent,
    - a parameter is sent with an incorrect value,
    - an unexpected parameter is sent.
 - *403-Forbidden*: When we don't recognise the user agent supplied to us, your User agent needs to be added to our allow list.
 - *404-Not found*: when an unknown URI is accessed.
 - *405-Bad method*: when an unacceptable method is used.
 - *500-Internal server error*: if a fault happened in the address lookup application
 - *500-Internal server error*: if the address lookup application could not access its database

No upstream services are consumed, so 502 will never be returned.

In each error case, the body will be a short message intended to assist the developer (not to
be shown to end users), with `Content-Type: text/plain`.

## 3. UK Address Search

Two methods are provided for searching for addresses. In both cases, a list of zero or more
addresses is returned. The response format is a _JSON array_ containing *zero or more*
[UK Address Objects (v2)](uk-address-object.json). [Example response (v2)](example-response-multiple.json)

If an address is a PO Box the response will contain an optional attribute named `poBox`, see the [PO Box example response](example-response-pobox.json).

### 3A. Lookup by Postcode

This is an endpoint that searches for addresses at a given postcode.

URL:

 - `{contextPath}/lookup

Example URL:

 - `{contextPath}/lookup`
   ```json
   {
      "postcode": "AA1 1ZZ",
      "filter": "The Rectory"
   }
   ```

Methods:

 - `POST`

Headers:

 - `User-Agent` (required): *string*

   This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
   string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
   problems with default User-Agent values.
   It will be used for reporting. All requests from a given origin must carry the same code-name.

 - `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

   Example:

     - `Accept-Language: cy, en`

   If no match was made, the default response will be sent, which will typically be English.

   Note that clients of this service that have user-facing UIs may pass the Accept-Language
   header sent by the user-agent through directly.

Body:

   ```json
   {
      "postcode": "AA1 1ZZ",
      "filter": "The Rectory"
   }
   ```
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
 
### 3B. Lookup by Town

This is an endpoint that searches for addresses within a given postal town

URL:

 - `{contextPath}/lookup/by-post-town

Example URL:

 - `{contextPath}/lookup/by-post-town
   ```json
   {
      "posttown": "Test town",
      "filter": "The Rectory"
   }
   ```

Methods:

 - `POST` 

Headers:

 - `User-Agent` (required): *string*

   This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
   string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
   problems with default User-Agent values.
   It will be used for reporting. All requests from a given origin must carry the same code-name.

 - `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

   Example:

     - `Accept-Language: cy, en`

   If no match was made, the default response will be sent, which will typically be English.

   Note that clients of this service that have user-facing UIs may pass the Accept-Language
   header sent by the user-agent through directly.

Body:
   ```json
   {
      "posttown": "Test town",
      "filter": "The Rectory"
   }
   ```
 - `posttown` (required): the postal town of the address.
 - `filter` (optional): a sub-string match on any of the address lines.

Status codes:

 - *200-OK* when the search was successful (n.b. response might be `[]`)
 - others as in section 1.

Response:

 - `Content-Type: application/json`
 - Expiry and cache control headers will be set appropriately.
 - The body will be a _JSON array_ containing *zero or more* [UK Address Objects (v2)](uk-address-object.json).
     [Example response (v2)](example-response-multiple.json)
     
### 3C. Lookup by UPRN

This is an endpoint that searches for the address(es) of a given UPRN.

URL:

 - `{contextPath}/lookup/by-uprn`

Example URL:

 - `{contextPath}/lookup/by-uprn`
   ```json
      {
        "uprn": "1234512345"
      }
   ```

Methods:

 - `POST`

Headers:

 - `User-Agent` (required): *string*

   This identifies the origin of the request so that usage patterns can be tracked. The value will be a short
   string containing some code-name of the originating service, e.g. `yta`. It must not contain '/' to avoid
   problems with default User-Agent values.
   It will be used for reporting. All requests from a given origin must carry the same code-name.

 - `Accept-Language` (optional): two-letter ISO639-1 case-insensitive code list

   Example:

     - `Accept-Language: cy, en`

   If no match was made, the default response will be sent, which will typically be English.

   Note that clients of this service that have user-facing UIs may pass the Accept-Language
   header sent by the user-agent through directly.

Body:
```json
{
   "uprn": "1234512345"
}
```
Status codes:

 - *200-OK* when the postcode search was successful (n.b. response might be `[]`)
 - others as in section 1.

Response:

 - `Content-Type: application/json`
 - Expiry and cache control headers will be set appropriately.
 - The body will be a _JSON array_ containing *zero or more* [UK Address Objects (v2)](uk-address-object.json).
     [Example response (v2)](example-response-multiple.json)


## 4. Liveness Test

URL:

 - `{contextPath}/ping`

Methods:

 - `GET`
 - `HEAD`

Headers:

 - (none)

Query params:

 - (none)

Status codes:

 - *200-OK*

Response:

 - `Content-Type: text/plain`
 - Expiry will be immediate and caching will be disabled.
 - The body will be a short text string that will describe the provenance of the server
   build including the Git version number etc.


## 5. Other Information

* Accept-Language header used here is as per [RFC-3066](https://tools.ietf.org/html/rfc3066)
   and [RFC-7231 5.3.5](https://tools.ietf.org/html/rfc7231#section-5.3.5).

* Query parameters must be correctly encoded: see
    [URL Encoding](https://en.wikipedia.org/wiki/Query_string#URL_encoding).

* Language codes are as per [ISO639](https://en.wikipedia.org/wiki/ISO_639)

* Country codes are as per the UK [Country Register](https://country.register.gov.uk/). This provides a list
    of two-letter country codes that is very similar to [ISO3166-1](https://en.wikipedia.org/wiki/ISO_3166-1).

* Country subdivisions are as per [ISO3166-2](https://en.wikipedia.org/wiki/ISO_3166-2).
  These are GB-ENG, GB-SCT, GB-NIR, GB-WLS.

Jersey, Guernsey and IoM do not have a subdivision and their country code is set to JE, GG, and IM accordingly.