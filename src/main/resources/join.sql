SELECT u.CallSign,ex.Date,ex.Description,ev.feedbackCount,ev.feedbackText
FROM Users u
INNER JOIN Events ev ON u.userIdx = ev.userIdx
INNER JOIN Exercises ex ON ev.exerciseIdx = ex.exerciseIdx
WHERE CallSign IN ('KM6SO')
AND Date >= '2026-01-01'
ORDER By ex.Date DESC, u.CallSign ASC

