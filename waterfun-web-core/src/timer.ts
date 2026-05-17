import {ISOString} from "~/types";
import {dayjs} from "element-plus";

export const formatDate = (dateObj: any) => {
  if (!dateObj) return '';
  let date;
  if (typeof dateObj === 'string') {
    // ISO 8601
    date = new Date(dateObj);
  } else if (dateObj.seconds !== undefined) {
    // Timestamp
    date = new Date(dateObj.seconds * 1000 + dateObj.nanos / 1000000);
  } else {
    date = new Date(dateObj);
  }
  return isNaN(date.getTime()) ? '' : date.toLocaleString('zh-CN');
};

export const formatISOData = (isodate: ISOString) => {
  const date = dayjs(isodate);
  return date.format('YYYY-MM-DD HH:mm')  // 2026-03-25 09:30
}