ALTER TABLE entries ADD FULLTEXT KEY ft_entry_content (title, content) WITH PARSER ngram;

