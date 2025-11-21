-- Introduce master tables for Tipe_Kamar, Fasilitas, and their joins to align with ERD.

CREATE TABLE IF NOT EXISTS tipe_kamar (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(60) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fasilitas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL UNIQUE,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS kamar_fasilitas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  facility_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_kamar_fasilitas_room FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_kamar_fasilitas_facility FOREIGN KEY (facility_id) REFERENCES fasilitas(id)
) ENGINE=InnoDB;

-- Seed tipe_kamar based on existing enum values (if not present)
INSERT INTO tipe_kamar (code, name)
SELECT code, name
FROM (
  SELECT 'DELUXE_KING' AS code, 'Deluxe King' AS name UNION ALL
  SELECT 'DELUXE_TWIN', 'Deluxe Twin' UNION ALL
  SELECT 'SUITE_PANORAMA', 'Suite Panorama' UNION ALL
  SELECT 'SUPERIOR_TWIN', 'Superior Twin' UNION ALL
  SELECT 'STUDIO_LOFT', 'Studio Loft' UNION ALL
  SELECT 'EXECUTIVE_SUITE', 'Executive Suite' UNION ALL
  SELECT 'FAMILY_ROOM', 'Family Room' UNION ALL
  SELECT 'PRESIDENTIAL_SUITE', 'Presidential Suite' UNION ALL
  SELECT 'STANDARD', 'Standard' UNION ALL
  SELECT 'VILLA', 'Villa'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM tipe_kamar tk WHERE tk.code = seed.code);

-- Map existing rooms to tipe_kamar via a helper join table (kept separate to avoid altering rooms schema)
CREATE TABLE IF NOT EXISTS kamar_tipe (
  id BIGINT NOT NULL AUTO_INCREMENT,
  room_id BIGINT NOT NULL UNIQUE,
  tipe_kamar_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_kamar_tipe_room FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT fk_kamar_tipe_type FOREIGN KEY (tipe_kamar_id) REFERENCES tipe_kamar(id)
) ENGINE=InnoDB;

INSERT INTO kamar_tipe (room_id, tipe_kamar_id)
SELECT r.id, tk.id
FROM rooms r
JOIN tipe_kamar tk ON tk.code = r.type
WHERE NOT EXISTS (SELECT 1 FROM kamar_tipe kt WHERE kt.room_id = r.id);

-- Seed fasilitas from existing room_amenities
INSERT INTO fasilitas (name)
SELECT DISTINCT ra.name
FROM room_amenities ra
WHERE ra.name IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM fasilitas f WHERE f.name = ra.name
);

-- Link kamar_fasilitas from existing amenities
INSERT INTO kamar_fasilitas (room_id, facility_id)
SELECT ra.room_id, f.id
FROM room_amenities ra
JOIN fasilitas f ON f.name = ra.name
WHERE NOT EXISTS (
  SELECT 1 FROM kamar_fasilitas kf WHERE kf.room_id = ra.room_id AND kf.facility_id = f.id
);
