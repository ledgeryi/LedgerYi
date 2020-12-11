package cn.ledgeryi.framework.core.services.http;

import cn.ledgeryi.framework.common.application.Service;
import cn.ledgeryi.framework.core.config.args.Args;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "API")
public class LedgerYiNodeHttpApiService implements Service {

  private int port = Args.getInstance().getLedgerYiNodeHttpPort();

  private Server server;

  @Autowired
  private GetAccountServlet getAccountServlet;
  @Autowired
  private TransferServlet transferServlet;
  @Autowired
  private BroadcastServlet broadcastServlet;
  @Autowired
  private ListNodesServlet listNodesServlet;
  @Autowired
  private GetNowBlockServlet getNowBlockServlet;
  @Autowired
  private GetBlockByNumServlet getBlockByNumServlet;
  @Autowired
  private GetBlockByLimitNextServlet getBlockByLimitNextServlet;
  @Autowired
  private GetTransactionByIdServlet getTransactionByIdServlet;
  @Autowired
  private GetTransactionCountByBlockNumServlet getTransactionCountByBlockNumServlet;
  @Autowired
  private ListMastersServlet listMastersServlet;
  @Autowired
  private ValidateAddressServlet validateAddressServlet;
  @Autowired
  private GetNodeInfoServlet getNodeInfoServlet;
  @Autowired
  private CreateCommonTransactionServlet createTransactionServlet;

  @Override
  public void init() {
  }

  @Override
  public void init(Args args) {
  }

  @Override
  public void start() {
    try {
      server = new Server(port);
      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/v1/");
      server.setHandler(context);

      //node
      context.addServlet(new ServletHolder(listNodesServlet), "/getnodes");
      context.addServlet(new ServletHolder(listMastersServlet), "/getmasters");
      context.addServlet(new ServletHolder(getNodeInfoServlet), "/getnodeinfo");

      //account
      context.addServlet(new ServletHolder(getAccountServlet), "/getaccount");

      //block
      context.addServlet(new ServletHolder(getNowBlockServlet), "/getnowblock");
      context.addServlet(new ServletHolder(getBlockByNumServlet), "/getblockbynum");
      context.addServlet(new ServletHolder(getBlockByLimitNextServlet), "/getblockbylimitnext");

      //transaction
      context.addServlet(new ServletHolder(transferServlet), "/createtransfertransaction");
      context.addServlet(new ServletHolder(createTransactionServlet),"/createtransaction");
      context.addServlet(new ServletHolder(broadcastServlet), "/broadcasttransaction");
      context.addServlet(new ServletHolder(getTransactionByIdServlet), "/gettransactionbyid");
      context.addServlet(new ServletHolder(getTransactionCountByBlockNumServlet), "/gettransactioncountbyblocknum");

      //others
      context.addServlet(new ServletHolder(validateAddressServlet), "/validateaddress");

      int maxHttpConnectNumber = Args.getInstance().getMaxHttpConnectNumber();
      if (maxHttpConnectNumber > 0) {
        server.addBean(new ConnectionLimit(maxHttpConnectNumber, server));
      }
      server.start();
    } catch (Exception e) {
      log.debug("IOException: {}", e.getMessage());
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      log.debug("IOException: {}", e.getMessage());
    }
  }
}
