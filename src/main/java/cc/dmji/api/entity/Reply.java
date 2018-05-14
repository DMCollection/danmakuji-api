package cc.dmji.api.entity;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by echisan on 2018/5/14
 */
@Entity
@Table(name = "dm_reply", schema = "dmji", catalog = "")
public class Reply {
    private String replyId;
    private String content;
    private Integer rPage;
    private Timestamp createTime;
    private Timestamp modifyTime;
    private String userId;
    private String rStatus;
    private String parentId;
    private Byte isParent;
    private Integer rLike;
    private Integer rHate;

    @Id
    @Column(name = "reply_id")
    public String getReplyId() {
        return replyId;
    }

    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    @Basic
    @Column(name = "content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Basic
    @Column(name = "r_page")
    public Integer getrPage() {
        return rPage;
    }

    public void setrPage(Integer rPage) {
        this.rPage = rPage;
    }

    @Basic
    @Column(name = "create_time")
    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    @Basic
    @Column(name = "modify_time")
    public Timestamp getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Timestamp modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Basic
    @Column(name = "user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "r_status")
    public String getrStatus() {
        return rStatus;
    }

    public void setrStatus(String rStatus) {
        this.rStatus = rStatus;
    }

    @Basic
    @Column(name = "parent_id")
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Basic
    @Column(name = "is_parent")
    public Byte getIsParent() {
        return isParent;
    }

    public void setIsParent(Byte isParent) {
        this.isParent = isParent;
    }

    @Basic
    @Column(name = "r_like")
    public Integer getrLike() {
        return rLike;
    }

    public void setrLike(Integer rLike) {
        this.rLike = rLike;
    }

    @Basic
    @Column(name = "r_hate")
    public Integer getrHate() {
        return rHate;
    }

    public void setrHate(Integer rHate) {
        this.rHate = rHate;
    }

}
