-- DOES THIS EVEN GET USED???
CREATE INDEX abp_blpu_uprn_index ON __schema__.abp_blpu (uprn);
CREATE INDEX abp_delivery_point_urpn_index ON __schema__.abp_delivery_point(uprn);
CREATE INDEX abp_delivery_point_postcode_index ON __schema__.abp_delivery_point(postcode);
CREATE INDEX abp_lpi_urpn_index ON __schema__.abp_lpi(uprn);
CREATE INDEX abp_crossref_urpn_index ON __schema__.abp_crossref(uprn);
CREATE INDEX abp_classification_urpn_index ON __schema__.abp_classification(uprn);
CREATE INDEX abp_street_usrn_index ON __schema__.abp_street(usrn);
CREATE INDEX abp_street_descriptor_usrn_index ON __schema__.abp_street_descriptor(usrn);
CREATE INDEX abp_organisation_urpn_index ON __schema__.abp_organisation(uprn);
CREATE INDEX abp_successor_urpn_index ON __schema__.abp_successor(uprn);
