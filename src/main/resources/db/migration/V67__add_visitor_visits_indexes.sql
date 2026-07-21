CREATE INDEX IF NOT EXISTS idx_visitor_visits_site ON visitor_visits (assigned_site);
CREATE INDEX IF NOT EXISTS idx_visitor_visits_guest_user_id ON visitor_visits (guest_user_id);
CREATE INDEX IF NOT EXISTS idx_visitor_visits_status ON visitor_visits (status);
