# coding=UTF-8

from ztest.internal_celesta_unit import InternalCelestaUnit
from ztest._ztest_orm import viewWithGetDateCursor
from ztest._ztest_orm import tableForGetDateInViewCursor
from java.sql import Timestamp
from java.time import LocalDateTime


class TestGetDate(InternalCelestaUnit):
    def test_getdate_in_view(self):
        viewCursor = viewWithGetDateCursor(self.context)
        self.assertEqual(0, viewCursor.count())

        tableCursor = tableForGetDateInViewCursor(self.context)

        tableCursor.date = Timestamp.valueOf(LocalDateTime.now().minusDays(1))
        tableCursor.insert()
        self.assertEqual(0, viewCursor.count())

        tableCursor.clear()
        tableCursor.date = Timestamp.valueOf(LocalDateTime.now().plusDays(1))
        tableCursor.insert()
        self.assertEqual(1, viewCursor.count())
