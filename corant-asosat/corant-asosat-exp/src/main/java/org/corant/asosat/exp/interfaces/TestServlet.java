/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.exp.interfaces;

import static org.corant.shared.util.MapUtils.getMapString;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import org.corant.asosat.exp.application.TestApplicationService;
import org.corant.asosat.exp.application.TestQueryService;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午8:18:27
 *
 */
@ApplicationScoped
@WebServlet(urlPatterns = "/test")
public class TestServlet extends HttpServlet {

  private static final long serialVersionUID = 8174294172579816895L;

  @Inject
  TestApplicationService as;

  @Inject
  TestQueryService qs;

  @Transactional
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    as.testEntityManager();
    StringBuilder sb = new StringBuilder("<table>");
    List<Map<String, Object>> list = qs.select("Industries.query", null);
    list.forEach(m -> {
      sb.append("<tr><td>").append(getMapString(m, "name")).append("</td></tr>");
    });
    sb.append("</table>");
    resp.getWriter().write(sb.toString());
  }

}
