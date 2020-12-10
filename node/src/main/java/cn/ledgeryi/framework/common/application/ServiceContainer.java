package cn.ledgeryi.framework.common.application;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import cn.ledgeryi.framework.core.config.args.Args;

@Slf4j(topic = "app")
public class ServiceContainer {

  private ArrayList<Service> services;

  public ServiceContainer() {
    this.services = new ArrayList<>();
  }

  public void add(Service service) {
    this.services.add(service);
  }


  public void init() {
    for (Service service : this.services) {
      log.debug("Initing " + service.getClass().getSimpleName());
      service.init();
    }
  }

  public void init(Args args) {
    for (Service service : this.services) {
      log.debug("Initing " + service.getClass().getSimpleName());
      service.init(args);
    }
  }

  public void start() {
    log.debug("Starting services");
    for (Service service : this.services) {
      log.debug("Starting " + service.getClass().getSimpleName());
      service.start();
    }
  }

  public void stop() {
    for (Service service : this.services) {
      log.debug("Stopping " + service.getClass().getSimpleName());
      service.stop();
    }
  }
}
