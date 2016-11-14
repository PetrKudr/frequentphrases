DROP FUNCTION IF EXISTS Capitalize;
CREATE FUNCTION Capitalize(str VARCHAR(255)) RETURNS VARCHAR(255) 
BEGIN  
  DECLARE res VARCHAR(255);
  DECLARE c CHAR(1);    
  DECLARE i INT DEFAULT 1;  
  DECLARE insideWord INT DEFAULT 0;
  SET res = LCASE(str);  
  WHILE i <= LENGTH(str) DO
    BEGIN  
      SET c = SUBSTRING(res, i, 1);  
      IF (c >= 'a' AND c <= 'z') OR (c >= '0' AND c <= '9') OR (c = '_') THEN 
        IF insideWord = 0 THEN
          BEGIN
            SET res = CONCAT(LEFT(res, i - 1), UCASE(c), SUBSTRING(res, i + 1));
            SET insideWord = 1;
          END;
        END IF;
      ELSE
        SET insideWord = 0;
      END IF;
      SET i = i + 1;  
    END;  
  END WHILE;  
  RETURN res;  
END; 