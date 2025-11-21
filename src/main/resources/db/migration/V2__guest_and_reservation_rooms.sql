-- Creates Guest (tamu) table and Reservation_Rooms bridge per ERD.
-- Safe to run multiple times on MySQL 8+: uses IF NOT EXISTS and NOT EXISTS inserts.

CREATE TABLE IF NOT EXISTS tamu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_tamu_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservation_rooms (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  nightly_rate DECIMAL(12,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_reservation_rooms_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
  CONSTRAINT fk_reservation_rooms_room FOREIGN KEY (room_id) REFERENCES rooms(id)
) ENGINE=InnoDB;

-- Backfill guest profiles for existing users
INSERT INTO tamu (user_id, created_at)
SELECT u.id, COALESCE(u.created_at, NOW())
FROM users u
WHERE NOT EXISTS (
  SELECT 1 FROM tamu t WHERE t.user_id = u.id
);

-- Backfill reservation_rooms for existing single-room reservations
INSERT INTO reservation_rooms (reservation_id, room_id, nightly_rate)
SELECT r.id, r.room_id, COALESCE(rm.rate, 0)
FROM reservations r
JOIN rooms rm ON rm.id = r.room_id
WHERE NOT EXISTS (
  SELECT 1 FROM reservation_rooms rr WHERE rr.reservation_id = r.id
);
