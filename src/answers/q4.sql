DROP PROCEDURE IF EXISTS ShowOpenBugs;
CREATE PROCEDURE ShowOpenBugs(fromDate DATE, toDate DATE)
BEGIN
  DROP TEMPORARY TABLE IF EXISTS daterange;
  CREATE TEMPORARY TABLE daterange (dte DATE); 

  SET @counter := -1;
  WHILE (@counter < DATEDIFF(toDate, fromDate)) DO 
    INSERT daterange VALUES (DATE_ADD(fromDate, INTERVAL @counter:=@counter + 1 DAY));
  END WHILE;

  SELECT dte, COUNT(bugs.id) from daterange
  INNER JOIN bugs ON bugs.open_date <= daterange.dte AND (bugs.close_date > daterange.dte OR bugs.close_date IS NULL)
  GROUP BY dte;
END;

-- CALL ShowOpenBugs(STR_TO_DATE('2010-01-01', '%Y-%m-%d'), STR_TO_DATE('2010-01-30', '%Y-%m-%d'));

