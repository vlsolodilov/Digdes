package com.solodilov.vladislav;

import com.digdes.school.DatesToCronConvertException;
import com.digdes.school.DatesToCronConverter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DatesToCronConverterImpl implements DatesToCronConverter {
  private boolean isEnd = false;

  public DatesToCronConverterImpl() {
  }

  @Override
  public String convert(List<String> list) throws DatesToCronConvertException {

    //преобразование списка из строк в даты
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
    List<LocalDateTime> dates = new ArrayList<>();
    try {
      for (String s : list) {
        dates.add(LocalDateTime.parse(s, formatter));
      }
    } catch (Exception e) {
      throw new DatesToCronConvertException();
    }

    //составление мапы интервалов между датами и списком этих дат
    Map<Duration, List<LocalDateTime>> durationListMap = new HashMap<>();
    for (int i = 0; i < dates.size() - 1; i++) {
      for (int j = i + 1; j < dates.size(); j++) {
        Duration duration = Duration.between(dates.get(i), dates.get(j));
        if (!durationListMap.containsKey(duration)) {
          durationListMap.put(duration, new ArrayList<>());
        }
        durationListMap.get(duration).add(dates.get(i));
        durationListMap.get(duration).add(dates.get(j));
      }
    }

    //нахождение максимального количества повторяющегося интервала между датами
    //размер списка напрямую связан с количеством повторяющегося интервала между датами
    int maxSize = 0;
    Duration resDuration = null;
    for (Duration d : durationListMap.keySet()) {
      int size = durationListMap.get(d).size();
      if (size > maxSize) {
        maxSize = size;
        resDuration = d;
      }
    }

    //удаление дубликатов и сортировка списка
    List<LocalDateTime> resList;
    if (resDuration != null) {
      resList = durationListMap.get(resDuration).stream()
          .distinct()
          .sorted()
          .collect(Collectors.toList());
    } else {
      throw new DatesToCronConvertException();
    }

    //проверка, что максимальное количество дат больше половины в списке
    if (resList.size() < list.size() / 2) {
      throw new DatesToCronConvertException();
    }

    //получение period по duration
    Period resPeriod = null;
    for (int i = 0; i < resList.size() - 1; i++) {
      if (Duration.between(resList.get(i), resList.get(i + 1)).equals(resDuration)){
        resPeriod = Period.between(resList.get(i).toLocalDate(),
            resList.get(i + 1).toLocalDate());
      }
    }

    isEnd = false;
    return String.format("%s %s %s %s %s %s",
        getCronSeconds(resList, resDuration),
        getCronMinutes(resList, resDuration),
        getCronHours(resList, resDuration),
        getCronDays(resList, resPeriod),
        getCronMonth(resList, resPeriod),
        getDayOfWeek(resList));
  }

  @Override
  public String getImplementationInfo() {
    return "Solodilov Vladislav Vyacheslavovich, "
        + DatesToCronConverterImpl.class.getSimpleName() + ", "
        + DatesToCronConverterImpl.class.getPackage().getName() + ", "
        + "https://github.com/vlsolodilov";
  }

  private String getCronSeconds(List<LocalDateTime> list, Duration duration)
      throws DatesToCronConvertException {
    long sec = duration.getSeconds() % 60;
    if (sec == 0) {
      return String.valueOf(list.get(0).getSecond());
    } else if (sec == 1) {
      isEnd = true;
      return "*";
    } else if (sec > 1 && sec < 30) {
      return String.format("*/%d", sec);
    } else if (sec >= 30) {
      TreeSet<Integer> secondSet = new TreeSet<>();
      for (LocalDateTime ldt : list) {
        secondSet.add(ldt.getSecond());
      }
      return String.format("%d/%d", secondSet.first(), sec);
    } else
      throw new DatesToCronConvertException();
  }

  private String getCronMinutes(List<LocalDateTime> list, Duration duration)
      throws DatesToCronConvertException {
    long minutes = (duration.getSeconds() / 60) % 60;
    if (isEnd){
      return "*";
    }
    if (duration.getSeconds() / 60 == 0) {
      isEnd = true;
    }
    if (minutes == 0) {
      return String.valueOf(list.get(0).getMinute());
    } else if (minutes == 1) {
      isEnd = true;
      return "*";
    } else if (minutes > 0 && minutes < 30) {
      return String.format("*/%d", minutes);
    } else if (minutes >= 30) {
      TreeSet<Integer> minuteSet = new TreeSet<>();
      for (LocalDateTime ldt : list) {
        minuteSet.add(ldt.getMinute());
      }
      return String.format("%d/%d", minuteSet.first(), minutes);
    } else
      throw new DatesToCronConvertException();
  }

  private String getCronHours(List<LocalDateTime> list, Duration duration)
      throws DatesToCronConvertException {
    long hours = (duration.getSeconds() / (60 * 60)) % 24;
    if (isEnd){
      return "*";
    }
    if (duration.getSeconds() / (60 * 60) == 0) {
      isEnd = true;
    }
    TreeSet<Integer> hourSet = new TreeSet<>();
    for (LocalDateTime ldt : list) {
      hourSet.add(ldt.getHour());
    }
    int min = hourSet.first();
    int max = hourSet.last();
    if (hours == 0) {
      if (min != max) {
        return String.format("%d-%d", min, max);
      } else {
        return String.valueOf(min);
      }
    } else if (hours > 0) {
      if (hourSet.size() == 24 / hours) {
        if (hours == 1) {
          isEnd = true;
          return "*";
        } else {
          return String.format("*/%d", hours);
        }
      } else {
        return getStringFromSet(hourSet);
      }
    } else
      throw new DatesToCronConvertException();
  }

  private String getCronDays(List<LocalDateTime> list, Period period)
      throws DatesToCronConvertException {
    int days = period.getDays();
    if (isEnd){
      return "*";
    }
    if (period.getMonths() == 0) {
      isEnd = true;
    }
    TreeSet<Integer> daySet = new TreeSet<>();
    for (LocalDateTime ldt : list) {
      daySet.add(ldt.getDayOfMonth());
    }
    int min = daySet.first();
    int max = daySet.last();
    if (days == 0) {
      if (min != max) {
        return String.format("%d-%d", min, max);
      } else {
        return String.valueOf(min);
      }
    } else if (days > 0) {
      if ((max - min) == (days * (daySet.size() - 1))) {
        if (days == 1){
          isEnd = true;
          return "*";
        } else {
          return String.format("*/%d", days);
        }
      } else {
        return getStringFromSet(daySet);
      }
    } else {
      throw new DatesToCronConvertException();
    }
  }

  private String getCronMonth(List<LocalDateTime> list, Period period)
      throws DatesToCronConvertException {
    int months = period.getMonths();
    if (isEnd){
      return "*";
    }
    TreeSet<Integer> monthSet = new TreeSet<>();
    for (LocalDateTime ldt : list) {
      monthSet.add(ldt.getMonthValue());
    }
    int min = monthSet.first();
    int max = monthSet.last();
    if (months == 0) {
      if (min != max) {
        return String.format("%d-%d", min, max);
      } else {
        return String.valueOf(min);
      }
    } else if (months > 0) {
      if (monthSet.size() == (12 / months)) {
        if (months == 1) {
          return "*";
        } else {
          return String.format("*/%d", months);
        }
      } else {
        return getStringFromSet(monthSet);
      }
    } else {
      throw new DatesToCronConvertException();
    }
  }

  private String getDayOfWeek(List<LocalDateTime> list) {
    for (int i = 1; i < list.size(); i++) {
      if (!list.get(0).getDayOfWeek().equals(list.get(i).getDayOfWeek())){
        return "*";
      }
    }
    return list.get(0).getDayOfWeek().toString().substring(0,3);
  }

  private String getStringFromSet(TreeSet<Integer> set) {
    Iterator<Integer> itr = set.iterator();
    StringBuilder sb = new StringBuilder();
    while (itr.hasNext()) {
      sb.append(itr.next()).append(",");
    }
    sb.deleteCharAt(sb.lastIndexOf(","));
    return sb.toString();
  }
}
