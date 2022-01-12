/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.json.expression.ast;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Objects.areEqual;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.modules.json.expression.Node;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-json
 *
 * @author bingo 下午9:28:11
 *
 */
public interface ASTComparisonNode extends ASTPredicateNode {

  ASTNode<?> getLeft();

  ASTNode<?> getRight();

  abstract class AbstractASTComparisonNode implements ASTComparisonNode {

    protected final List<ASTNode<?>> children = new ArrayList<>();
    protected final ASTNodeType type;

    protected AbstractASTComparisonNode(ASTNodeType type) {
      this.type = type;
    }

    @Override
    public boolean addChild(Node<?> child) {
      return children.add((ASTNode<?>) child);
    }

    @Override
    public List<? extends Node<?>> getChildren() {
      return children;
    }

    @Override
    public ASTNode<?> getLeft() {
      return children.get(0);
    }

    @Override
    public ASTNode<?> getRight() {
      return children.get(1);
    }

    @Override
    public ASTNodeType getType() {
      return type;
    }

    protected int compare(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);// FIXME wrap?
      Object right = getRightValue(ctx);// FIXME wrap?
      if (left instanceof Comparable && right instanceof Comparable) {
        return compare(left, right);
      }
      throw new NotSupportedException();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected int compare(Object left, Object right) {
      if (left instanceof Number && right instanceof Number) {
        Number numberLeft = (Number) left;
        Number numberRight = (Number) right;
        if (numberLeft instanceof BigDecimal || numberRight instanceof BigDecimal) {
          return compare(numberLeft, numberRight, BigDecimal.class);
        } else if (numberLeft instanceof Double || numberRight instanceof Double) {
          return Double.compare(numberLeft.doubleValue(), numberRight.doubleValue());
        } else if (numberLeft instanceof Float || numberRight instanceof Float) {
          return Float.compare(numberLeft.floatValue(), numberRight.floatValue());
        } else if (numberLeft instanceof BigInteger || numberRight instanceof BigInteger) {
          return compare(numberLeft, numberRight, BigInteger.class);
        } else if (numberLeft instanceof Long || numberRight instanceof Long) {
          return Long.compare(numberLeft.longValue(), numberRight.longValue());
        } else if (numberLeft instanceof Integer || numberRight instanceof Integer) {
          return Integer.compare(numberLeft.intValue(), numberRight.intValue());
        } else if (numberLeft instanceof Short || numberRight instanceof Short) {
          return Short.compare(numberLeft.shortValue(), numberRight.shortValue());
        } else if (numberLeft instanceof Byte || numberRight instanceof Byte) {
          return Byte.compare(numberLeft.byteValue(), numberRight.byteValue());
        } else {
          return compare(left, right, Double.class);
        }
      } else if (left instanceof TemporalAccessor && right instanceof TemporalAccessor) {
        if (left instanceof Date && right instanceof Date) {
          return ((Date) left).compareTo((Date) right);
        } else if (left instanceof Instant && right instanceof Instant) {
          return ((Instant) left).compareTo((Instant) right);
        } else if (left instanceof ZonedDateTime && right instanceof ZonedDateTime) {
          return ((ZonedDateTime) left).compareTo((ZonedDateTime) right);
        } else if (left instanceof LocalTime && right instanceof LocalTime) {
          return ((LocalTime) left).compareTo((LocalTime) right);
        } else if (left instanceof LocalDate && right instanceof LocalDate) {
          return ((LocalDate) left).compareTo((LocalDate) right);
        } else if (left instanceof LocalDateTime && right instanceof LocalDateTime) {
          return ((LocalDateTime) left).compareTo((LocalDateTime) right);
        } else if (left instanceof MonthDay && right instanceof MonthDay) {
          return ((MonthDay) left).compareTo((MonthDay) right);
        } else if (left instanceof OffsetDateTime && right instanceof OffsetDateTime) {
          return ((OffsetDateTime) left).compareTo((OffsetDateTime) right);
        } else if (left instanceof OffsetTime && right instanceof OffsetTime) {
          return ((OffsetTime) left).compareTo((OffsetTime) right);
        } else if (left instanceof Year && right instanceof Year) {
          return ((Year) left).compareTo((Year) right);
        } else if (left instanceof YearMonth && right instanceof YearMonth) {
          return ((YearMonth) left).compareTo((YearMonth) right);
        } else if (left instanceof ZoneOffset && right instanceof ZoneOffset) {
          return ((ZoneOffset) left).compareTo((ZoneOffset) right);
        }
      } else if (left instanceof String && right instanceof String) {
        return left.toString().compareTo(right.toString());
      } else if (left instanceof Duration && right instanceof Duration) {
        return ((Duration) left).compareTo((Duration) right);
      } else if (areEqual(left.getClass(), right.getClass())) {
        return ((Comparable) left).compareTo(right);
      }
      throw new NotSupportedException();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected int compare(Object left, Object right, Class<? extends Comparable> clazz) {
      return toObject(left, clazz).compareTo(toObject(right, clazz));
    }

    protected Object getLeftValue(EvaluationContext ctx) {
      return getLeft().getValue(ctx);
    }

    protected Object getRightValue(EvaluationContext ctx) {
      return getRight().getValue(ctx);
    }
  }

  class ASTBetweenNode extends AbstractASTComparisonNode {

    public ASTBetweenNode() {
      super(ASTNodeType.CP_BTW);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object min = getChildren().get(1).getValue(ctx);
      Object mid = getChildren().get(0).getValue(ctx);
      Object max = getChildren().get(2).getValue(ctx);
      return compare(min, mid) <= 0 && compare(mid, max) <= 0;
    }

  }

  class ASTEqualNode extends AbstractASTComparisonNode {

    public ASTEqualNode() {
      super(ASTNodeType.CP_EQ);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      Object right = getRightValue(ctx);
      if (left instanceof Number && right instanceof Number) {
        return compare(left, right) == 0;
      }
      return areEqual(left, right);
    }
  }

  class ASTGreaterThanEqualNode extends AbstractASTComparisonNode {

    public ASTGreaterThanEqualNode() {
      super(ASTNodeType.CP_GTE);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return super.compare(ctx) >= 0;
    }
  }

  class ASTGreaterThanNode extends AbstractASTComparisonNode {

    public ASTGreaterThanNode() {
      super(ASTNodeType.CP_GT);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return super.compare(ctx) > 0;
    }
  }

  class ASTInNode extends AbstractASTComparisonNode {

    public ASTInNode() {
      super(ASTNodeType.CP_IN);
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      Object right = getRightValue(ctx);
      if (!(right instanceof Collection)) {
        return false;
      }
      return ((Collection) right).contains(left);
    }
  }

  class ASTLessThanEqualNode extends AbstractASTComparisonNode {

    public ASTLessThanEqualNode() {
      super(ASTNodeType.CP_LTE);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return super.compare(ctx) <= 0;
    }
  }

  class ASTLessThanNode extends AbstractASTComparisonNode {

    public ASTLessThanNode() {
      super(ASTNodeType.CP_LT);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      return super.compare(ctx) < 0;
    }
  }

  class ASTNoEqualNode extends AbstractASTComparisonNode {

    public ASTNoEqualNode() {
      super(ASTNodeType.CP_NE);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      Object right = getRightValue(ctx);
      if (left instanceof Number && right instanceof Number) {
        return compare(left, right) != 0;
      }
      return !areEqual(left, right);
    }
  }

  class ASTNoInNode extends AbstractASTComparisonNode {

    public ASTNoInNode() {
      super(ASTNodeType.CP_NIN);
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      Object right = getRightValue(ctx);
      if (!(right instanceof Collection)) {
        return true;
      }
      return !((Collection) right).contains(left);
    }
  }

  class ASTRegexNode extends AbstractASTComparisonNode {

    protected Pattern pattern;

    public ASTRegexNode() {
      super(ASTNodeType.CP_REGEX);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      if (left == null) {
        return false;
      } else {
        return pattern.matcher(left.toString()).matches();
      }
    }

    public void initialize() {
      shouldBeTrue(sizeOf(getChildren()) == 2
          && ((ASTNode<?>) getChildren().get(1)).getType() == ASTNodeType.VAL);
      pattern = Pattern.compile(((ASTValueNode) getChildren().get(1)).getValue(null).toString());
    }
  }

}
