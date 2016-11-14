DROP PROCEDURE IF EXISTS SplitColumnsIntoRows;
CREATE PROCEDURE SplitColumnsIntoRows(delim CHAR) 
BEGIN
  DECLARE origID INT;    
  DECLARE string VARCHAR(255);
  DECLARE idx INT DEFAULT 0;  
  DECLARE slice VARCHAR(255);  
  DECLARE done INT DEFAULT 0;
  Declare tableCursor CURSOR FOR SELECT id, name FROM sometbl;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;

  DROP TEMPORARY TABLE IF EXISTS temptable;
  CREATE TEMPORARY TABLE temptable (id INT, name VARCHAR(50)); 

  OPEN tableCursor;
  FETCH tableCursor INTO origID, string;

  WHILE done = 0 DO
      SET idx = 0;
      IF string IS NOT NULL THEN
        BEGIN
          SET idx = LOCATE(delim, string);
          WHILE idx != 0 AND LENGTH(string) > 0 DO
            IF idx != 0 THEN
              SET slice = LEFT(string, idx - 1);     
            else     
              SET slice = string;
            END IF;
 
            INSERT INTO temptable VALUES(origID, slice);
            --IF (LENGTH(slice) > 0) THEN  
            --  INSERT INTO temptable VALUES(origID, slice);   
            --END IF;
 
            SET string = RIGHT(string, LENGTH(string) - idx);      
            SET idx = LOCATE(delim, string);
          END WHILE; 

          INSERT INTO temptable VALUES(origID, string);   
          --IF (LENGTH(string) > 0) THEN
          --  INSERT INTO temptable VALUES(origID, string);   
          --END IF; 
        END;
      ELSE 
        INSERT INTO temptable VALUES(origID, null);
      END IF;
      
      FETCH tableCursor INTO origID, string;
    
  END WHILE;
  
  SELECT * FROM temptable;
END; 