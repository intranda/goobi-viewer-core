CREATE INDEX index_users_email ON users (email);
CREATE INDEX index_comments_pi_page ON comments (pi,page);
CREATE INDEX index_overview_pages_pi ON overview_pages (pi);
CREATE INDEX index_overview_page_updates_pi ON overview_page_updates (pi);

CREATE INDEX index_crowdsourcing_fulltexts_pi ON crowdsourcing_fulltexts (pi);
CREATE INDEX index_crowdsourcing_fulltexts_pi_page ON crowdsourcing_fulltexts (pi,page);
CREATE INDEX index_crowdsourcing_fulltexts_pi_page_completed ON crowdsourcing_fulltexts (pi,page_completed);
CREATE INDEX index_crowdsourcing_user_generated_contents_pi ON crowdsourcing_user_generated_contents (pi);
CREATE INDEX index_crowdsourcing_user_generated_contents_pi_page ON crowdsourcing_user_generated_contents (pi,page);
CREATE INDEX index_crowdsourcing_user_generated_contents_pi_page_completed ON crowdsourcing_user_generated_contents (pi,page_completed);