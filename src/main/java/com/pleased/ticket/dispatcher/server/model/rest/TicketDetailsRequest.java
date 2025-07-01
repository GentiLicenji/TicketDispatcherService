package com.pleased.ticket.dispatcher.server.model.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Objects;

/**
 * TicketDetailsRequest
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-06-29T19:58:10.280+02:00")


public class TicketDetailsRequest   {
  @JsonProperty("subject")
  private String subject = null;

  @JsonProperty("description")
  private String description = null;

  /**
   * Gets or Sets priority
   */
  public enum PriorityEnum {
    LOW("low"),
    
    MEDIUM("medium"),
    
    HIGH("high"),
    
    URGENT("urgent");

    private String value;

    PriorityEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static PriorityEnum fromValue(String text) {
      for (PriorityEnum b : PriorityEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("priority")
  private PriorityEnum priority = null;

  @JsonProperty("notify")
  private Boolean notify = null;

  @JsonProperty("dueDate")
  private LocalDate dueDate = null;

  public TicketDetailsRequest subject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Get subject
   * @return subject
  **/
  @ApiModelProperty(value = "")


  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public TicketDetailsRequest description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  **/
  @ApiModelProperty(value = "")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TicketDetailsRequest priority(PriorityEnum priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Get priority
   * @return priority
  **/
  @ApiModelProperty(value = "")


  public PriorityEnum getPriority() {
    return priority;
  }

  public void setPriority(PriorityEnum priority) {
    this.priority = priority;
  }

  public TicketDetailsRequest notify(Boolean notify) {
    this.notify = notify;
    return this;
  }

  /**
   * Get notify
   * @return notify
  **/
  @ApiModelProperty(value = "")


  public Boolean isNotify() {
    return notify;
  }

  public void setNotify(Boolean notify) {
    this.notify = notify;
  }

  public TicketDetailsRequest dueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
    return this;
  }

  /**
   * Get dueDate
   * @return dueDate
  **/
  @ApiModelProperty(value = "")

  @Valid

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TicketDetailsRequest ticketDetailsRequest = (TicketDetailsRequest) o;
    return Objects.equals(this.subject, ticketDetailsRequest.subject) &&
        Objects.equals(this.description, ticketDetailsRequest.description) &&
        Objects.equals(this.priority, ticketDetailsRequest.priority) &&
        Objects.equals(this.notify, ticketDetailsRequest.notify) &&
        Objects.equals(this.dueDate, ticketDetailsRequest.dueDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, description, priority, notify, dueDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TicketDetailsRequest {\n");
    
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    notify: ").append(toIndentedString(notify)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

