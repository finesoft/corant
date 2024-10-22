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
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;
import org.corant.modules.json.expression.EvaluationContext;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Iterables;

/**
 * corant-modules-json
 *
 * @author bingo 下午9:28:11
 */
public interface ASTComparisonNode extends ASTPredicateNode {

  ASTNode<?> getLeft();

  ASTNode<?> getRight();

  abstract class AbstractASTComparisonNode extends AbstractASTNode<Boolean>
      implements ASTComparisonNode {

    protected final ASTNodeType type;

    protected AbstractASTComparisonNode(ASTNodeType type) {
      this.type = type;
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
      throw new NotSupportedException("Only supports comparable objects for comparison node");
    }

    @SuppressWarnings("unchecked")
    protected int compare(Object left, Object right) {
      if (left instanceof Number ln && right instanceof Number rn) {
        if (ln instanceof BigDecimal || rn instanceof BigDecimal) {
          return compare(ln, rn, BigDecimal.class);
        } else if (ln instanceof Double || rn instanceof Double) {
          return Double.compare(ln.doubleValue(), rn.doubleValue());
        } else if (ln instanceof Float || rn instanceof Float) {
          return Float.compare(ln.floatValue(), rn.floatValue());
        } else if (ln instanceof BigInteger || rn instanceof BigInteger) {
          return compare(ln, rn, BigInteger.class);
        } else if (ln instanceof Long || rn instanceof Long) {
          return Long.compare(ln.longValue(), rn.longValue());
        } else if (ln instanceof Integer || rn instanceof Integer) {
          return Integer.compare(ln.intValue(), rn.intValue());
        } else if (ln instanceof Short || rn instanceof Short) {
          return Short.compare(ln.shortValue(), rn.shortValue());
        } else if (ln instanceof Byte || rn instanceof Byte) {
          return Byte.compare(ln.byteValue(), rn.byteValue());
        } else {
          return compare(left, right, Double.class);
        }
      } else if (left instanceof TemporalAccessor && right instanceof TemporalAccessor) {
        if (left instanceof Date ld && right instanceof Date rd) {
          return ld.compareTo(rd);
        } else if (left instanceof Instant li && right instanceof Instant ri) {
          return li.compareTo(ri);
        } else if (left instanceof ZonedDateTime lz && right instanceof ZonedDateTime rz) {
          return lz.compareTo(rz);
        } else if (left instanceof LocalTime llt && right instanceof LocalTime rlt) {
          return llt.compareTo(rlt);
        } else if (left instanceof LocalDate lld && right instanceof LocalDate rld) {
          return lld.compareTo(rld);
        } else if (left instanceof LocalDateTime lldt && right instanceof LocalDateTime rldt) {
          return lldt.compareTo(rldt);
        } else if (left instanceof MonthDay lmd && right instanceof MonthDay rmd) {
          return lmd.compareTo(rmd);
        } else if (left instanceof OffsetDateTime lodt && right instanceof OffsetDateTime rodt) {
          return lodt.compareTo(rodt);
        } else if (left instanceof OffsetTime lot && right instanceof OffsetTime rot) {
          return lot.compareTo(rot);
        } else if (left instanceof Year ly && right instanceof Year ry) {
          return ly.compareTo(ry);
        } else if (left instanceof YearMonth lym && right instanceof YearMonth rym) {
          return lym.compareTo(rym);
        } else if (left instanceof ZoneOffset lzo && right instanceof ZoneOffset rzo) {
          return lzo.compareTo(rzo);
        }
      } else if (left instanceof String && right instanceof String) {
        return left.toString().compareTo(right.toString());
      } else if (left instanceof Duration ld && right instanceof Duration rd) {
        return ld.compareTo(rd);
      }
      if (left instanceof Comparable lc && areEqual(left.getClass(), right.getClass())) {
        return lc.compareTo(right);
      }
      throw new NotSupportedException(
          "Only supports same type comparable objects for comparison node");
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
      if (left != null && left.equals(right)) {
        return true;
      }
      if (left instanceof Number && right instanceof Number) {
        return compare(left, right) == 0;
      }
      return false;
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

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      Object right = getRightValue(ctx);
      if (right instanceof Collection<?> collection) {
        return collection.contains(left);
      } else if (right instanceof String rs && left instanceof String ls) {
        return rs.contains(ls);
      } else if (right instanceof Object[] array) {
        return Iterables.search(array, right) != -1;
      }
      return false;
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
      if (left == null || right == null) {
        return true;
      }
      if (left.equals(right)) {
        return false;
      }
      if (left instanceof Number && right instanceof Number) {
        return compare(left, right) != 0;
      }
      return true;
    }
  }

  class ASTNoInNode extends AbstractASTComparisonNode {

    public ASTNoInNode() {
      super(ASTNodeType.CP_NIN);
    }

    @Override
    public Boolean getValue(EvaluationContext ctx) {
      Object left = getLeftValue(ctx);
      Object right = getRightValue(ctx);
      if (right instanceof Collection<?> collection) {
        return !collection.contains(left);
      } else if (right instanceof String rs && left instanceof String ls) {
        return !rs.contains(ls);
      } else if (right instanceof Object[] array) {
        return Iterables.search(array, right) == -1;
      }
      return true;
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

    @Override
    public void postConstruct() {
      shouldBeTrue(sizeOf(getChildren()) == 2
          && ((ASTNode<?>) getChildren().get(1)).getType() == ASTNodeType.VALUE);
      pattern = Pattern.compile(((ASTValueNode) getChildren().get(1)).getValue(null).toString());
    }
  }

}
