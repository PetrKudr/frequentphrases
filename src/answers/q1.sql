SET @rank = 0;
SELECT @rank:=@rank+1 AS rank, name, votes FROM votes ORDER BY votes DESC;
