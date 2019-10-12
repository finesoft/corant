package org.corant.microprofile.restclient;

import static org.corant.shared.util.ConversionUtils.toEnum;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.corant.suites.json.JsonUtils;
import javax.ws.rs.ext.ContextResolver;
import java.io.IOException;

/**
 * cps-m2b <br>
 *
 * @auther sushuaihao 2019/8/30
 * @since
 */
public class MpDefaultContextResolver implements ContextResolver<ObjectMapper> {

  static ObjectMapper om;

  static {
    om = JsonUtils.copyMapper().registerModules(new SimpleModule().setDeserializerModifier(new EnumBeanDeserializerModifier()));
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return om;
  }

  @Deprecated
  static final class EnumBeanDeserializerModifier extends BeanDeserializerModifier {
    @SuppressWarnings("rawtypes")
    @Override
    public JsonDeserializer<Enum> modifyEnumDeserializer(DeserializationConfig config,
                                                         final JavaType type, BeanDescription beanDesc, final JsonDeserializer<?> deserializer) {
      return new JsonDeserializer<Enum>() {
        @SuppressWarnings("unchecked")
        @Override
        public Enum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
          Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();
          if (jp.currentToken() == JsonToken.VALUE_STRING
                  || jp.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return toEnum(jp.getValueAsString(), rawClass);
          } else if (jp.currentToken() == JsonToken.START_OBJECT) {
            JsonNode node = jp.getCodec().readTree(jp);
            return toEnum(node.get("name").asText(), rawClass);
          }
          return null;
        }
      };
    }
  }
}
