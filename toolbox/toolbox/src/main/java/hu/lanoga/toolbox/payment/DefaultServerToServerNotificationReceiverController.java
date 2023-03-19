package hu.lanoga.toolbox.payment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class DefaultServerToServerNotificationReceiverController extends ServerToServerNotificationReceiverController {

    @Override
    @RequestMapping(value = ServerToServerNotificationReceiverController.S2S_URL, method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT })
    public void receive(String paymentTransactionGid, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        super.receive(paymentTransactionGid, httpServletRequest, httpServletResponse);
    }
}
