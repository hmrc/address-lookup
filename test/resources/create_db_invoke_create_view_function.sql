BEGIN TRANSACTION;
SELECT create_address_lookup_view('__schema__')
  INTO __schema__.tmptbl;
COMMIT;