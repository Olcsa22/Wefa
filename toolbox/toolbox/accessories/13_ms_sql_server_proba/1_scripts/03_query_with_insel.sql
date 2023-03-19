SELECT insel.* FROM

(
SELECT
id, 
--(SELECT [dbo].[extr_from_lang]((SELECT x.kutya FROM cica x WHERE x.id = y.id), 'hu', 'en')) as json_caption
([dbo].[extr_from_lang](kutya, 'hu', 'en')) as json_caption
FROM cica
WHERE ID < 10000
--ORDER BY ID ASC
) insel
 
WHERE insel.json_caption like '%a1%' 
ORDER BY insel.id desc

OFFSET 10 ROWS 
FETCH NEXT 10 ROWS ONLY 
