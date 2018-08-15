package cc.dmji.api.web.controller.v2;

import cc.dmji.api.common.Result;
import cc.dmji.api.common.ResultCode;
import cc.dmji.api.entity.User;
import cc.dmji.api.entity.v2.MessageV2;
import cc.dmji.api.entity.v2.SysMessage;
import cc.dmji.api.enums.MessageType;
import cc.dmji.api.enums.Role;
import cc.dmji.api.enums.Status;
import cc.dmji.api.enums.v2.SysMsgTargetType;
import cc.dmji.api.service.SysMessageService;
import cc.dmji.api.service.UserService;
import cc.dmji.api.service.v2.MessageV2Service;
import cc.dmji.api.utils.DmjiUtils;
import cc.dmji.api.utils.PageInfo;
import cc.dmji.api.web.controller.BaseController;
import cc.dmji.api.web.model.v2.message.MessageDetail;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/v2/messages")
public class MessageV2Controller extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(MessageV2Controller.class);
    @Autowired
    private MessageV2Service messageV2Service;
    @Autowired
    private SysMessageService sysMessageService;
    @Autowired
    private UserService userService;

    @GetMapping("/cum")
    public ResponseEntity<Result> countUnreadMessage(HttpServletRequest request) {
        Long uid = getUidFromRequest(request);
        User user = userService.getUserById(uid);
        Role role = Role.byRoleName(user.getRole());
        // 未读回复通知
        Long unReadReplyMessageCount = messageV2Service.countUnreadMessage(uid, MessageType.REPLY);
        // 未读系统通知
        Long unReadSystemMessageCount = messageV2Service.countUnreadMessage(uid, MessageType.SYSTEM);
        // 未读的新系统通知
        Long newSysMessage = sysMessageService.countNewSysMessage(user.getUserId(), user.getCreateTime(), SysMsgTargetType.byUserRole(role));
        if (newSysMessage != 0) {
            List<SysMessage> sysMessages = sysMessageService.listNewSysMessages(user.getUserId(), user.getCreateTime(), SysMsgTargetType.byUserRole(role));
            if (sysMessages != null && sysMessages.size() != 0) {
                List<MessageV2> messageV2List = new ArrayList<>();
                sysMessages.forEach(sysMessage -> {
                    MessageV2 messageV2 = new MessageV2();
                    messageV2.setRead(false);
                    messageV2.setStatus(Status.NORMAL);
                    messageV2.setContent(sysMessage.getContent());
                    messageV2.setTitle(sysMessage.getTitle());
                    messageV2.setType(MessageType.SYSTEM);
                    messageV2.setCreateTime(sysMessage.getCreateTime());
                    messageV2.setPublisherUid(0L);
                    messageV2.setSysMessageId(sysMessage.getId());
                    messageV2.setUid(uid);
                    messageV2List.add(messageV2);
                });
                List<MessageV2> insertAll = messageV2Service.insertAll(messageV2List);
                logger.debug("新增加的系统通知:{}",insertAll);
            }
            unReadSystemMessageCount = unReadReplyMessageCount + newSysMessage;
        }

        Long unReadLikeMessageCount = messageV2Service.countUnreadMessage(uid, MessageType.LIKE);
        Long unReadAtMessageCount = messageV2Service.countUnreadMessage(uid, MessageType.AT);
        Long totalUnread = unReadReplyMessageCount + unReadSystemMessageCount + unReadLikeMessageCount + unReadAtMessageCount;

        Map<String, Long> data = new HashMap<>();
        data.put("reply", unReadReplyMessageCount);
        data.put("system", unReadSystemMessageCount);
        data.put("at", unReadAtMessageCount);
        data.put("like", unReadLikeMessageCount);
        data.put("total", totalUnread);

        return getSuccessResponseEntity(getSuccessResult(data));
    }

    @GetMapping
    public ResponseEntity<Result> listMessage(@RequestParam(value = "type") Integer messageTypeCode,
                                              @RequestParam(value = "pn", required = false, defaultValue = "1") Integer pn,
                                              @RequestParam(value = "ps", required = false, defaultValue = "20") Integer ps,
                                              HttpServletRequest request) {
        MessageType messageType = MessageType.byCode(messageTypeCode);
        if (messageType == null) {
            return getErrorResponseEntity(HttpStatus.NOT_FOUND, ResultCode.RESULT_DATA_NOT_FOUND, "不存在的messageType");
        }
        if (!DmjiUtils.validPageParam(pn, ps)) {
            return getErrorResponseEntity(HttpStatus.BAD_REQUEST, ResultCode.PARAM_IS_INVALID, "pn,ps参数no正确");
        }

        Long uid = getUidFromRequest(request);
        Page<MessageDetail> messageDetailPage =
                PageHelper.startPage(pn, ps, true).doSelectPage(() -> messageV2Service.listMessages(uid, messageType));

        PageInfo pageInfo = new PageInfo(pn, ps, messageDetailPage.getTotal());
        Map<String, Object> responseMap = new HashMap<>(4);
        responseMap.put("page", pageInfo);
        responseMap.put("messages", messageDetailPage.getResult());

        /* --------- 将未读的消息设置成已读 ---------- */
        cleanUnReadMessage(uid, messageType);
        /* --------- --------------------- ---------- */

        return getSuccessResponseEntity(getSuccessResult(responseMap));

    }

    @DeleteMapping("/{mid}")
    public ResponseEntity<Result> deleteMessage(@PathVariable("mid") Long messageId, HttpServletRequest request) {
        MessageV2 messageV2 = messageV2Service.getById(messageId);
        if (messageV2 == null) {
            return getErrorResponseEntity(HttpStatus.NOT_FOUND,
                    ResultCode.DATA_ALREADY_EXIST_BUT_ALLOW_REQUEST, "需要删除的内容不存在");
        }

        Long uid = getUidFromRequest(request);
        if (!uid.equals(messageV2.getUid())) {
            return getErrorResponseEntity(HttpStatus.FORBIDDEN, ResultCode.PERMISSION_DENY, "无权删除");
        }

        messageV2.setStatus(Status.DELETE);
        MessageV2 update = messageV2Service.update(messageV2);
        logger.debug("删除id为{}的消息,删除状态：{}", update.getId(), update.getStatus());
        return getSuccessResponseEntity(getSuccessResult());
    }

    /**
     * 清空某个messageType下的未读的消息
     *
     * @param userId 用户id、
     * @param type   messageType
     */
    @Async
    public void cleanUnReadMessage(Long userId, MessageType type) {
        List<MessageV2> messages = messageV2Service.listUserUnReadMessage(userId, type);
        if (messages.size() != 0) {
            messages.forEach(message -> message.setRead(true));
            List<MessageV2> messages1 = messageV2Service.insertAll(messages);
            logger.debug("已将id为{}的用户的未读信息设置为已读,共{}条", userId, messages1.size());
        }
    }

}
