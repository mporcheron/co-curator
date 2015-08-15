<?php

require_once 'db.php';

\requireInput('Must provide global user ID', ['globalUserId']);

$globalUserId = \intval($data['globalUserId']);

if ($stmt = $db->prepare('SELECT `localItemId`, `itemType`, `itemData`, `itemDateTime` FROM `item` WHERE `globalUserId`=:globalUserId')) {
	$stmt->bindParam(':globalUserId', $globalUserId, SQLITE3_INTEGER);

	if($res = $stmt->execute()) {
		$data = [];
		while($row = $res->fetchArray()) {
			$row['itemData'] = \removeUnicodeSequences($row['itemData']);

			$data[] = ['id' => $row['localItemId'],
				'type' => $row['itemType'],
				'data' => $row['itemData'],
				'dateTime' => $row['itemDateTime']];
		}

		$stmt->close();
	} else {
		\dieError($db->lastErrorMsg(), 'Internal Server Error');
	}

	\dieResult($data);
} else {
	\dieError($db->lastErrorMsg(), 'Internal Server Error');
}