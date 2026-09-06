package com.cobbleclub.client.claims.panel;

import com.cobbleclub.client.claims.ClaimsNetworking;
import com.cobbleclub.client.claims.ClaimsState;
import com.cobbleclub.client.ui.ClubScrollbar;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.client.ui.ThemedButton;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.ClaimMessagesEdit;
import com.cobbleclub.clubhouse.claims.protocol.MemberEntry;
import com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import net.minecraft.class_7919;

@Environment(EnvType.CLIENT)
public final class ClaimDetailPanel {
   private static final int ROW_H = 20;
   private static final int INFO_LINE_H = 12;
   private static final int INFO_LINES_BOTTOM = 74;
   private static final int SEGMENT_W = 16;
   private static final int PERMS_LEGEND_H = 14;
   private static final int MEMBER_HEADER_H = 22;
   private static final int MESSAGES_VIEW_ONLY_OFFSET = 16;
   private static final String CHIP_PARENT = "";
   private static final DateFormat DATE_MEDIUM;
   private static final DateFormat DATE_SHORT;
   private final ClaimsState state;
   private final class_327 font;
   private final Consumer<ClaimDetailEntry> showOnMap;
   private final Consumer<ClaimDetailEntry> teleport;
   private final ConfirmRequest confirm;
   private int x0;
   private int y0;
   private int x1;
   private int y1;
   private String claimId;
   private SubTab subTab;
   private String permSubId;
   private String infoSubId;
   private int scroll;
   private Pattern namePattern;
   private class_342 renameBox;
   private ThemedButton saveNameButton;
   private ThemedButton mapButton;
   private ThemedButton teleportButton;
   private class_342 transferBox;
   private ThemedButton transferButton;
   private ThemedButton deleteButton;
   private class_7919 deleteDenyTooltip;
   private class_342 trustBox;
   private ThemedButton trustButton;
   private ThemedButton banButton;
   private class_342 subNameBox;
   private ThemedButton subSaveButton;
   private ThemedButton subDeleteButton;
   private class_342 enterTitleBox;
   private class_342 enterSubtitleBox;
   private class_342 leaveTitleBox;
   private class_342 leaveSubtitleBox;
   private ThemedButton saveMessagesButton;
   private String renameText;
   private String transferText;
   private String trustText;
   private String subNameText;
   private String subNameSeededId;
   private String enterTitleText;
   private String enterSubtitleText;
   private String leaveTitleText;
   private String leaveSubtitleText;
   private ClaimsState.PermRow hoveredPermTooltip;

   public ClaimDetailPanel(ClaimsState state, class_327 font, Consumer<ClaimDetailEntry> showOnMap, Consumer<ClaimDetailEntry> teleport, ConfirmRequest confirm) {
      this.subTab = ClaimDetailPanel.SubTab.INFO;
      this.transferText = "";
      this.trustText = "";
      this.state = state;
      this.font = font;
      this.showOnMap = showOnMap;
      this.teleport = teleport;
      this.confirm = confirm;

      try {
         this.namePattern = state.nameRegex != null ? Pattern.compile(state.nameRegex) : null;
      } catch (PatternSyntaxException var7) {
         this.namePattern = null;
      }

   }

   public String selectedClaimId() {
      return this.claimId;
   }

   public void select(String claimId) {
      if (!Objects.equals(this.claimId, claimId)) {
         this.claimId = claimId;
         this.subTab = ClaimDetailPanel.SubTab.INFO;
         this.permSubId = null;
         this.infoSubId = null;
         this.scroll = 0;
         this.renameText = null;
         this.transferText = "";
         this.trustText = "";
         this.subNameText = null;
         this.subNameSeededId = null;
         this.enterTitleText = null;
         this.enterSubtitleText = null;
         this.leaveTitleText = null;
         this.leaveSubtitleText = null;
         this.syncEditBoxValues();
      }

   }

   public ClaimDetailEntry claim() {
      return this.state.claimById(this.claimId);
   }

   private boolean adminView() {
      return this.state.adminTargetName != null;
   }

   private boolean messagesReadOnly(ClaimDetailEntry claim) {
      return this.adminView() || !Boolean.TRUE.equals(claim.getCanEditMessages());
   }

   public void reconcile() {
      if (this.claimId != null && this.claim() == null) {
         this.claimId = null;
      }

      ClaimDetailEntry claim = this.claim();
      if (claim != null && this.infoSubId != null && ClaimsState.subById(claim, this.infoSubId) == null) {
         this.infoSubId = null;
      }

      this.syncEditBoxValues();
   }

   public void setBounds(int x0, int y0, int x1, int y1) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
   }

   public boolean contains(double mouseX, double mouseY) {
      return this.claimId != null && mouseX >= (double)this.x0 && mouseX < (double)this.x1 && mouseY >= (double)this.y0 && mouseY < (double)this.y1;
   }

   public void createWidgets(Consumer<class_339> add) {
      this.renameBox = new class_342(this.font, 0, 0, 100, 16, class_2561.method_43470("Claim name"));
      this.renameBox.method_1880(Math.max(1, this.state.maxNameLength));
      this.renameBox.method_1863((value) -> this.renameText = value);
      add.accept(this.renameBox);
      this.saveNameButton = new ThemedButton(0, 0, 48, 16, class_2561.method_43470("Save"), ThemedButton.Variant.GREEN, (b) -> this.saveRename());
      add.accept(this.saveNameButton);
      this.mapButton = new ThemedButton(0, 0, 46, 18, class_2561.method_43470("Map"), (b) -> {
         ClaimDetailEntry claim = this.claim();
         if (claim != null) {
            this.showOnMap.accept(claim);
         }

      });
      add.accept(this.mapButton);
      this.teleportButton = new ThemedButton(0, 0, 70, 18, class_2561.method_43470("Teleport"), ThemedButton.Variant.BLUE, (b) -> {
         ClaimDetailEntry claim = this.claim();
         if (claim != null) {
            this.teleport.accept(claim);
         }

      });
      add.accept(this.teleportButton);
      this.transferBox = new class_342(this.font, 0, 0, 100, 16, class_2561.method_43470("Transfer to"));
      this.transferBox.method_1880(16);
      this.transferBox.method_47404(class_2561.method_43470("Player name...").method_27692(class_124.field_1063));
      this.transferBox.method_1863((value) -> this.transferText = value);
      add.accept(this.transferBox);
      this.transferButton = new ThemedButton(0, 0, 70, 16, class_2561.method_43470("Transfer"), (b) -> this.transferClicked());
      add.accept(this.transferButton);
      this.deleteButton = new ThemedButton(0, 0, 70, 18, class_2561.method_43470("Delete"), ThemedButton.Variant.RED, (b) -> this.deleteClicked());
      add.accept(this.deleteButton);
      this.trustBox = new class_342(this.font, 0, 0, 100, 16, class_2561.method_43470("Trust player"));
      this.trustBox.method_1880(16);
      this.trustBox.method_47404(class_2561.method_43470("Player name...").method_27692(class_124.field_1063));
      this.trustBox.method_1863((value) -> this.trustText = value);
      add.accept(this.trustBox);
      this.trustButton = new ThemedButton(0, 0, 50, 16, class_2561.method_43470("Trust"), ThemedButton.Variant.GREEN, (b) -> this.trustClicked());
      add.accept(this.trustButton);
      this.banButton = new ThemedButton(0, 0, 44, 16, class_2561.method_43470("Ban"), ThemedButton.Variant.RED, (b) -> this.banClicked());
      add.accept(this.banButton);
      this.subNameBox = new class_342(this.font, 0, 0, 100, 16, class_2561.method_43470("Sub-claim name"));
      this.subNameBox.method_1880(Math.max(1, this.state.maxNameLength));
      this.subNameBox.method_1863((value) -> this.subNameText = value);
      add.accept(this.subNameBox);
      this.subSaveButton = new ThemedButton(0, 0, 48, 16, class_2561.method_43470("Save"), ThemedButton.Variant.GREEN, (b) -> this.saveSubRename());
      add.accept(this.subSaveButton);
      this.subDeleteButton = new ThemedButton(0, 0, 74, 18, class_2561.method_43470("Delete Sub"), ThemedButton.Variant.RED, (b) -> this.deleteSubClicked());
      add.accept(this.subDeleteButton);
      this.enterTitleBox = this.messageBox(add, (value) -> this.enterTitleText = value);
      this.enterSubtitleBox = this.messageBox(add, (value) -> this.enterSubtitleText = value);
      this.leaveTitleBox = this.messageBox(add, (value) -> this.leaveTitleText = value);
      this.leaveSubtitleBox = this.messageBox(add, (value) -> this.leaveSubtitleText = value);
      this.saveMessagesButton = new ThemedButton(0, 0, 60, 18, class_2561.method_43470("Save"), ThemedButton.Variant.GREEN, (b) -> this.saveMessages());
      add.accept(this.saveMessagesButton);
      this.syncEditBoxValues();
   }

   private class_342 messageBox(Consumer<class_339> add, Consumer<String> mirror) {
      class_342 box = new class_342(this.font, 0, 0, 100, 14, class_2561.method_43470("Message"));
      box.method_1880(Math.max(1, this.state.maxMessageLength));
      Objects.requireNonNull(mirror);
      box.method_1863(mirror::accept);
      add.accept(box);
      return box;
   }

   private void syncEditBoxValues() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null && this.renameBox != null) {
         if (this.renameText == null) {
            this.renameText = claim.getName() != null ? claim.getName() : "";
         }

         SubClaimEntry selectedSub = this.selectedSub(claim);
         String selectedSubId = selectedSub == null ? null : selectedSub.getSubId();
         if (!Objects.equals(selectedSubId, this.subNameSeededId)) {
            this.subNameSeededId = selectedSubId;
            this.subNameText = selectedSub == null ? null : this.seedSubName(selectedSub);
         }

         if (this.enterTitleText == null) {
            this.enterTitleText = orEmpty(claim.getEnterTitle());
         }

         if (this.enterSubtitleText == null) {
            this.enterSubtitleText = orEmpty(claim.getEnterSubtitle());
         }

         if (this.leaveTitleText == null) {
            this.leaveTitleText = orEmpty(claim.getLeaveTitle());
         }

         if (this.leaveSubtitleText == null) {
            this.leaveSubtitleText = orEmpty(claim.getLeaveSubtitle());
         }

         setIfDiffers(this.renameBox, this.renameText);
         setIfDiffers(this.transferBox, this.transferText);
         setIfDiffers(this.trustBox, this.trustText);
         setIfDiffers(this.subNameBox, orEmpty(this.subNameText));
         setIfDiffers(this.enterTitleBox, this.enterTitleText);
         setIfDiffers(this.enterSubtitleBox, this.enterSubtitleText);
         setIfDiffers(this.leaveTitleBox, this.leaveTitleText);
         setIfDiffers(this.leaveSubtitleBox, this.leaveSubtitleText);
      }
   }

   private static void setIfDiffers(class_342 box, String value) {
      if (box != null && !box.method_1882().equals(value)) {
         box.method_1852(value);
      }

   }

   private static String orEmpty(String s) {
      return s == null ? "" : s;
   }

   private String seedSubName(SubClaimEntry sub) {
      String name = orEmpty(sub.getName());
      return this.nameValid(name) ? name : "";
   }

   public boolean anyEditBoxFocused() {
      return this.focusedEditBox() != null;
   }

   public class_342 focusedEditBox() {
      for(class_342 box : this.editBoxes()) {
         if (box != null && box.method_25370()) {
            return box;
         }
      }

      return null;
   }

   public boolean submitFocusedEditBox() {
      if (this.renameBox != null && this.renameBox.method_25370()) {
         this.saveRename();
         return true;
      } else if (this.subNameBox != null && this.subNameBox.method_25370()) {
         this.saveSubRename();
         return true;
      } else if (this.trustBox != null && this.trustBox.method_25370()) {
         this.trustClicked();
         return true;
      } else if (this.transferBox != null && this.transferBox.method_25370()) {
         this.transferClicked();
         return true;
      } else {
         for(class_342 box : List.of(this.enterTitleBox, this.enterSubtitleBox, this.leaveTitleBox, this.leaveSubtitleBox)) {
            if (box != null && box.method_25370()) {
               this.saveMessages();
               return true;
            }
         }

         return false;
      }
   }

   public void unfocusAll(double mouseX, double mouseY) {
      for(class_342 box : this.editBoxes()) {
         if (box != null && box.method_25370() && !box.method_25405(mouseX, mouseY)) {
            box.method_25365(false);
         }
      }

   }

   private List<class_342> editBoxes() {
      List<class_342> boxes = new ArrayList();
      boxes.add(this.renameBox);
      boxes.add(this.transferBox);
      boxes.add(this.trustBox);
      boxes.add(this.subNameBox);
      boxes.add(this.enterTitleBox);
      boxes.add(this.enterSubtitleBox);
      boxes.add(this.leaveTitleBox);
      boxes.add(this.leaveSubtitleBox);
      return boxes;
   }

   public void updateWidgets() {
      ClaimDetailEntry claim = this.claim();
      boolean visible = claim != null;
      boolean owner = visible && claim.isOwner() && !this.adminView();
      boolean canManage = visible && claim.getCanManageMembers() && !this.adminView();
      int contentX = this.x0 + 6;
      int contentW = this.x1 - this.x0 - 12;
      int infoY = this.contentTop() - this.scroll;
      boolean info = visible && this.subTab == ClaimDetailPanel.SubTab.INFO;
      InfoLayout layout = visible ? this.infoLayout(claim) : ClaimDetailPanel.InfoLayout.EMPTY;
      this.place(this.renameBox, contentX, infoY + layout.nameRowY(), contentW - 54, info && owner);
      this.place(this.saveNameButton, this.x1 - 6 - 48, infoY + layout.nameRowY(), 48, info && owner);
      boolean showMap = info && this.state.isOnMap(this.claimId);
      boolean showDelete = info && owner;
      if (showDelete) {
         this.deleteButton.method_47400(this.canDelete(claim) ? null : this.deleteDenyTooltip());
      }

      int buttonX = contentX;
      boolean showTeleport = info && (this.adminView() || this.state.adminBypass);
      this.place(this.teleportButton, contentX, infoY + layout.buttonRowY(), 70, showTeleport);
      if (showTeleport) {
         buttonX = contentX + 74;
      }

      this.place(this.mapButton, buttonX, infoY + layout.buttonRowY(), 46, showMap);
      if (showMap) {
         buttonX += 50;
      }

      this.place(this.deleteButton, buttonX, infoY + layout.buttonRowY(), 56, showDelete);
      this.place(this.transferBox, contentX, infoY + layout.transferRowY(), contentW - 76, info && owner);
      this.place(this.transferButton, this.x1 - 6 - 70, infoY + layout.transferRowY(), 70, info && owner);
      boolean subSection = info && owner && layout.subNameRowY() >= 0;
      this.place(this.subNameBox, contentX, infoY + layout.subNameRowY(), contentW - 54, subSection);
      this.place(this.subSaveButton, this.x1 - 6 - 48, infoY + layout.subNameRowY(), 48, subSection);
      this.place(this.subDeleteButton, contentX, infoY + layout.subButtonRowY(), 74, subSection);
      boolean banShown = this.state.hasScreenText("confirm_ban_title");
      this.place(this.trustBox, contentX, this.contentTop(), contentW - (banShown ? 104 : 56), visible && this.subTab == ClaimDetailPanel.SubTab.MEMBERS && canManage);
      this.place(this.trustButton, this.x1 - 6 - (banShown ? 96 : 50), this.contentTop(), 50, visible && this.subTab == ClaimDetailPanel.SubTab.MEMBERS && canManage);
      this.place(this.banButton, this.x1 - 6 - 44, this.contentTop(), 44, visible && this.subTab == ClaimDetailPanel.SubTab.MEMBERS && canManage && banShown);
      boolean canMessages = visible && !this.messagesReadOnly(claim);
      int msgY = this.contentTop() - this.scroll;
      this.place(this.enterTitleBox, contentX, msgY + 12, contentW, visible && this.subTab == ClaimDetailPanel.SubTab.MESSAGES && canMessages);
      this.place(this.enterSubtitleBox, contentX, msgY + 44, contentW, visible && this.subTab == ClaimDetailPanel.SubTab.MESSAGES && canMessages);
      this.place(this.leaveTitleBox, contentX, msgY + 76, contentW, visible && this.subTab == ClaimDetailPanel.SubTab.MESSAGES && canMessages);
      this.place(this.leaveSubtitleBox, contentX, msgY + 108, contentW, visible && this.subTab == ClaimDetailPanel.SubTab.MESSAGES && canMessages);
      this.place(this.saveMessagesButton, contentX, msgY + 130, 60, visible && this.subTab == ClaimDetailPanel.SubTab.MESSAGES && canMessages);
   }

   private void place(class_339 widget, int x, int y, int width, boolean visible) {
      if (widget != null) {
         widget.field_22764 = visible;
         widget.field_22763 = visible;
         if (visible) {
            widget.method_46421(x);
            widget.method_46419(y);
            widget.method_25358(width);
            if (y < this.contentTop() - 20 || y > this.y1 - 8) {
               widget.field_22764 = false;
               widget.field_22763 = false;
            }

         }
      }
   }

   private InfoLayout infoLayout(ClaimDetailEntry claim) {
      int y = 74;
      if (claim.is3D()) {
         y += 12;
      }

      if (!claim.getLocal()) {
         y += 12;
      }

      boolean owner = claim.isOwner() && !this.adminView();
      int nameLabelY = -1;
      int nameRowY = -1;
      int buttonRowY = -1;
      int transferLabelY = -1;
      int transferRowY = -1;
      int subsLabelY = -1;
      int subChipsY = -1;
      int subNameRowY = -1;
      int subButtonRowY = -1;
      if (owner) {
         nameLabelY = y + 8;
         nameRowY = nameLabelY + 12;
         y = nameRowY + 22;
      }

      if (owner || this.state.isOnMap(this.claimId) || this.adminView()) {
         buttonRowY = y;
         y += 24;
      }

      if (owner) {
         transferLabelY = y;
         transferRowY = y + 12;
         y = transferRowY + 20;
      }

      if (owner && !subsOf(claim).isEmpty()) {
         subsLabelY = y + 6;
         subChipsY = subsLabelY + 12;
         subNameRowY = subChipsY + this.subChipRowsHeight(claim) + 2;
         subButtonRowY = subNameRowY + 20;
         y = subButtonRowY + 22;
      }

      return new InfoLayout(nameLabelY, nameRowY, buttonRowY, transferLabelY, transferRowY, subsLabelY, subChipsY, subNameRowY, subButtonRowY, y + 2);
   }

   private static List<SubClaimEntry> subsOf(ClaimDetailEntry claim) {
      return claim.getSubClaims() != null ? claim.getSubClaims() : List.of();
   }

   private SubClaimEntry selectedSub(ClaimDetailEntry claim) {
      SubClaimEntry first = null;

      for(SubClaimEntry sub : subsOf(claim)) {
         if (sub != null) {
            if (first == null) {
               first = sub;
            }

            if (this.infoSubId != null && this.infoSubId.equals(sub.getSubId())) {
               return sub;
            }
         }
      }

      return first;
   }

   private void forEachSubChip(ClaimDetailEntry claim, int startY, SubChipVisitor visitor) {
      int chipX = this.x0 + 6;
      int chipY = startY;

      for(SubClaimEntry sub : subsOf(claim)) {
         if (sub != null) {
            int w = this.chipWidth(sub.getName());
            if (chipX + w > this.x1 - 8 && chipX > this.x0 + 6) {
               chipX = this.x0 + 6;
               chipY += 14;
            }

            if (!visitor.visit(sub, chipX, chipY, w)) {
               return;
            }

            chipX += w + 4;
         }
      }

   }

   private int subChipRowsHeight(ClaimDetailEntry claim) {
      int[] bottom = new int[]{0};
      this.forEachSubChip(claim, 0, (sub, chipX, chipY, w) -> {
         bottom[0] = chipY + 14;
         return true;
      });
      return bottom[0];
   }

   private int headerBottom() {
      return this.y0 + 16;
   }

   private int tabsBottom() {
      return this.headerBottom() + 14;
   }

   private int contentTop() {
      return this.tabsBottom() + 4;
   }

   private boolean canManageMembers(ClaimDetailEntry claim) {
      return claim.getCanManageMembers() && !this.adminView();
   }

   private int memberListTop(ClaimDetailEntry claim) {
      return this.contentTop() + (this.canManageMembers(claim) ? 22 : 2);
   }

   private int clipTop(ClaimDetailEntry claim) {
      return this.subTab == ClaimDetailPanel.SubTab.MEMBERS ? this.memberListTop(claim) : this.contentTop();
   }

   private void saveRename() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null && this.renameText != null) {
         String trimmed = this.renameText.trim();
         if (this.nameValid(trimmed)) {
            ClaimsNetworking.sendRename(claim.getClaimId(), trimmed);
            PreviewUi.playClick();
         }
      }
   }

   private boolean nameValid(String name) {
      if (name.length() >= this.state.minNameLength && name.length() <= this.state.maxNameLength) {
         return this.namePattern == null || this.namePattern.matcher(name).matches();
      } else {
         return false;
      }
   }

   private boolean canDelete(ClaimDetailEntry claim) {
      return this.state.adminBypass || ClaimsState.standingInside(claim);
   }

   private class_2561 denyDeleteOutside() {
      return this.state.screenText("deny_delete_outside");
   }

   private class_7919 deleteDenyTooltip() {
      if (this.deleteDenyTooltip == null) {
         this.deleteDenyTooltip = class_7919.method_47407(this.denyDeleteOutside());
      }

      return this.deleteDenyTooltip;
   }

   private void deleteClicked() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null) {
         if (!this.canDelete(claim)) {
            this.state.showBanner(this.denyDeleteOutside(), true);
         } else {
            String id = claim.getClaimId();
            this.confirm.request(this.state.screenText("confirm_delete_title"), this.state.screenText("confirm_delete_body", "name", claim.getName()), () -> ClaimsNetworking.sendDelete(id));
         }
      }
   }

   private void transferClicked() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null) {
         String name = this.transferText.trim();
         if (!name.isEmpty() && name.length() <= 16) {
            String id = claim.getClaimId();
            this.confirm.request(this.state.screenText("confirm_transfer_title"), this.state.screenText("confirm_transfer_body", "name", claim.getName(), "player", name), () -> ClaimsNetworking.sendTransfer(id, name));
         }
      }
   }

   private void trustClicked() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null) {
         String name = this.trustText.trim();
         if (!name.isEmpty() && name.length() <= 16) {
            this.state.optimisticTrust(claim.getClaimId(), name);
            this.trustText = "";
            if (this.trustBox != null) {
               this.trustBox.method_1852("");
            }

            ClaimsNetworking.sendTrust(claim.getClaimId(), name);
            PreviewUi.playClick();
         }
      }
   }

   private void banClicked() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null) {
         String name = this.trustText.trim();
         if (!name.isEmpty() && name.length() <= 16) {
            String id = claim.getClaimId();
            this.confirm.request(this.state.screenText("confirm_ban_title"), this.state.screenText("confirm_ban_body", "player", name), () -> {
               this.trustText = "";
               if (this.trustBox != null) {
                  this.trustBox.method_1852("");
               }

               ClaimsNetworking.sendBan(id, name);
            });
            PreviewUi.playClick();
         }
      }
   }

   private void saveSubRename() {
      ClaimDetailEntry claim = this.claim();
      SubClaimEntry sub = claim != null ? this.selectedSub(claim) : null;
      if (sub != null && this.subNameText != null) {
         String trimmed = this.subNameText.trim();
         if (this.nameValid(trimmed)) {
            ClaimsNetworking.sendRenameSub(claim.getClaimId(), sub.getSubId(), trimmed);
            PreviewUi.playClick();
         }
      }
   }

   private void deleteSubClicked() {
      ClaimDetailEntry claim = this.claim();
      SubClaimEntry sub = claim != null ? this.selectedSub(claim) : null;
      if (sub != null) {
         String id = claim.getClaimId();
         String subId = sub.getSubId();
         this.confirm.request(this.state.screenText("confirm_delete_sub_title"), this.state.screenText("confirm_delete_sub_body", "name", sub.getName()), () -> ClaimsNetworking.sendDeleteSub(id, subId));
      }
   }

   private void saveMessages() {
      ClaimDetailEntry claim = this.claim();
      if (claim != null) {
         String enterTitle = changed(this.enterTitleText, claim.getEnterTitle());
         String enterSubtitle = changed(this.enterSubtitleText, claim.getEnterSubtitle());
         String leaveTitle = changed(this.leaveTitleText, claim.getLeaveTitle());
         String leaveSubtitle = changed(this.leaveSubtitleText, claim.getLeaveSubtitle());
         if (enterTitle != null || enterSubtitle != null || leaveTitle != null || leaveSubtitle != null) {
            ClaimsNetworking.sendSetMessages(claim.getClaimId(), new ClaimMessagesEdit(enterTitle, enterSubtitle, leaveTitle, leaveSubtitle));
            PreviewUi.playClick();
         }
      }
   }

   private static String changed(String edited, String current) {
      if (edited == null) {
         return null;
      } else {
         String base = current == null ? "" : current;
         return edited.equals(base) ? null : edited;
      }
   }

   public void render(class_332 g, int mouseX, int mouseY) {
      ClaimDetailEntry claim = this.claim();
      if (claim != null) {
         this.scroll = class_3532.method_15340(this.scroll, 0, this.maxScroll(claim));
         // Fully opaque backing prevents the claim-list text underneath from bleeding through.
         g.method_25294(this.x0 - 3, this.y0 - 3, this.x1 + 3, this.y1 + 3, 0xFF080D18);
         g.method_25294(this.x0 - 3, this.y0 - 3, this.x1 + 3, this.y1 + 3, 1426063360);
         g.method_25296(this.x0, this.y0, this.x1, this.y1, -199614136, -200601562);
         g.method_49601(this.x0 - 1, this.y0 - 1, this.x1 - this.x0 + 2, this.y1 - this.y0 + 2, -16447985);
         g.method_49601(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0, -13747610);
         g.method_25296(this.x0, this.y0, this.x1, this.headerBottom(), -14405538, -15459782);
         g.method_44379(this.x0 + 4, this.y0, this.x1 - 16, this.headerBottom());
         g.method_27535(this.font, class_2561.method_43470(claim.getName()), this.x0 + 5, this.y0 + 4, -2053377);
         g.method_44380();
         boolean crossHover = mouseX >= this.x1 - 14 && mouseX < this.x1 - 2 && mouseY >= this.y0 + 2 && mouseY < this.y0 + 14;
         g.method_25303(this.font, "✕", this.x1 - 12, this.y0 + 4, crossHover ? -1 : -7035976);
         boolean stripHover = mouseY >= this.headerBottom() && mouseY < this.tabsBottom();
         SubTab hoveredTab = stripHover ? this.subTabAt((double)mouseX) : null;
         int tabX = this.x0 + 4;

         for(SubTab tab : ClaimDetailPanel.SubTab.values()) {
            int w = this.tabWidth(tab);
            boolean active = tab == this.subTab;
            g.method_25303(this.font, this.tabLabel(tab), tabX + 4, this.headerBottom() + 3, active ? -2053377 : (tab == hoveredTab ? -1 : -7035976));
            if (active) {
               g.method_25294(tabX + 2, this.tabsBottom() - 1, tabX + w - 2, this.tabsBottom(), -6467875);
            }

            tabX += w;
         }

         g.method_44379(this.x0 + 1, this.clipTop(claim), this.x1 - 1, this.y1 - 1);
         switch (this.subTab.ordinal()) {
            case 0 -> this.renderInfo(g, claim, mouseX, mouseY);
            case 1 -> this.renderPerms(g, claim, mouseX, mouseY);
            case 2 -> this.renderMembers(g, claim, mouseX, mouseY);
            case 3 -> this.renderMessages(g, claim);
         }

         g.method_44380();
         ClubScrollbar.draw(g, this.x1 - 5, this.clipTop(claim), this.y1 - 4, this.scroll, this.maxScroll(claim));
      }
   }

   private String tabLabel(SubTab tab) {
      String var10000;
      switch (tab.ordinal()) {
         case 0 -> var10000 = "Details";
         case 1 -> var10000 = "Perms";
         case 2 -> var10000 = "Members";
         case 3 -> var10000 = "Messages";
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private int tabWidth(SubTab tab) {
      return this.font.method_1727(this.tabLabel(tab)) + 8;
   }

   private SubTab subTabAt(double mouseX) {
      int tabX = this.x0 + 4;

      for(SubTab tab : ClaimDetailPanel.SubTab.values()) {
         int w = this.tabWidth(tab);
         if (mouseX >= (double)tabX && mouseX < (double)(tabX + w)) {
            return tab;
         }

         tabX += w;
      }

      return null;
   }

   private void renderInfo(class_332 g, ClaimDetailEntry claim, int mouseX, int mouseY) {
      int x = this.x0 + 6;
      int y = this.contentTop() - this.scroll;
      int lineY = y + 2;
      g.method_27535(this.font, line("Owner: ", claim.getOwnerName()), x, lineY, -1);
      lineY += 12;
      g.method_27535(this.font, line("Where: ", claim.getZone()), x, lineY, -1);
      lineY += 12;
      int centerX = (claim.getBox().getMinX() + claim.getBox().getMaxX()) / 2;
      int centerZ = (claim.getBox().getMinZ() + claim.getBox().getMaxZ()) / 2;
      g.method_27535(this.font, line("Center: ", centerX + ", " + centerZ), x, lineY, -1);
      lineY += 12;
      int w = claim.getBox().getMaxX() - claim.getBox().getMinX() + 1;
      int d = claim.getBox().getMaxZ() - claim.getBox().getMinZ() + 1;
      g.method_27535(this.font, line("Size: ", w + "x" + d + " (" + ClaimsState.fmtBlocks(claim.getArea()) + " blocks)"), x, lineY, -1);
      lineY += 12;
      g.method_27535(this.font, line("Created: ", DATE_MEDIUM.format(new Date(claim.getCreatedAt()))), x, lineY, -1);
      lineY += 12;
      int subs = claim.getSubClaims() != null ? claim.getSubClaims().size() : 0;
      g.method_27535(this.font, line("Sub-Claims: ", String.valueOf(subs)), x, lineY, -1);
      lineY += 12;
      if (claim.is3D()) {
         g.method_27535(this.font, class_2561.method_43470("3D Claim").method_27692(class_124.field_1075), x, lineY, -1);
         lineY += 12;
      }

      if (!claim.getLocal()) {
         g.method_27535(this.font, class_2561.method_43470("Another World — Manage Only").method_27692(class_124.field_1065), x, lineY, -1);
      }

      if (claim.isOwner() && !this.adminView()) {
         InfoLayout layout = this.infoLayout(claim);
         g.method_27535(this.font, class_2561.method_43470("Name").method_27692(class_124.field_1080), x, y + layout.nameLabelY(), -1);
         int var10000 = this.renameText == null ? 0 : this.renameText.trim().length();
         String counter = var10000 + "/" + this.state.maxNameLength;
         boolean valid = this.renameText != null && this.nameValid(this.renameText.trim());
         g.method_25303(this.font, counter, this.x1 - 6 - this.font.method_1727(counter), y + layout.nameLabelY(), valid ? -7035976 : -2734768);
         g.method_27535(this.font, class_2561.method_43470("Transfer Ownership").method_27692(class_124.field_1080), x, y + layout.transferLabelY(), -1);
         if (layout.subsLabelY() >= 0) {
            g.method_27535(this.font, class_2561.method_43470("Sub-Claims").method_27692(class_124.field_1080), x, y + layout.subsLabelY(), -1);
            var10000 = this.subNameText == null ? 0 : this.subNameText.trim().length();
            String subCounter = var10000 + "/" + this.state.maxNameLength;
            boolean subValid = this.subNameText != null && this.nameValid(this.subNameText.trim());
            g.method_25303(this.font, subCounter, this.x1 - 6 - this.font.method_1727(subCounter), y + layout.subsLabelY(), subValid ? -7035976 : -2734768);
            SubClaimEntry selected = this.selectedSub(claim);
            this.forEachSubChip(claim, y + layout.subChipsY(), (sub, chipX, chipY, chipW) -> {
               boolean active = selected != null && sub.getSubId().equals(selected.getSubId());
               boolean hover = mouseX >= chipX && mouseX < chipX + chipW && mouseY >= chipY && mouseY < chipY + 12;
               this.drawSubChip(g, chipX, chipY, sub.getName(), active, hover);
               return true;
            });
         }
      }

   }

   private static class_2561 line(String label, String value) {
      return class_2561.method_43470(label).method_27692(class_124.field_1080).method_10852(class_2561.method_43470(value == null ? "?" : value).method_27692(class_124.field_1068));
   }

   private boolean canEditPerms(ClaimDetailEntry claim) {
      return !this.adminView() && Boolean.TRUE.equals(claim.getCanEditPermissions());
   }

   private boolean canAssignRole(ClaimDetailEntry claim, String currentRole, String role) {
      if (claim.isOwner()) {
         return true;
      } else if (role != null && !"OWNER".equals(role)) {
         return !"OWNER".equals(currentRole);
      } else {
         return false;
      }
   }

   private String effectiveRole(ClaimDetailEntry claim, String permission) {
      String role = this.state.effectivePermRole(claim, this.permSubId, permission);
      return role != null ? role : this.state.effectivePermRole(claim, (String)null, permission);
   }

   private void renderPerms(class_332 g, ClaimDetailEntry claim, int mouseX, int mouseY) {
      boolean editable = this.canEditPerms(claim);
      int y = this.contentTop() - this.scroll;
      g.method_27535(this.font, this.permsLegend(), this.x0 + 6, y + 2, -7035976);
      y += 14;
      List<SubClaimEntry> subs = claim.getSubClaims() != null ? claim.getSubClaims() : List.of();
      if (!subs.isEmpty()) {
         String hoveredChip = mouseY >= y && mouseY < y + 12 ? this.chipAt(subs, (double)mouseX) : null;
         int chipX = this.x0 + 6;
         chipX = this.drawSubChip(g, chipX, y, "Parent", this.permSubId == null, "".equals(hoveredChip));

         for(SubClaimEntry sub : subs) {
            if (sub != null) {
               chipX = this.drawSubChip(g, chipX, y, sub.getName(), Objects.equals(this.permSubId, sub.getSubId()), sub.getSubId().equals(hoveredChip));
            }
         }

         y += 16;
      }

      ClaimsState.PermRow hoveredRow = null;

      for(ClaimsState.PermRow row : this.state.permissionCatalog) {
         if (y + 20 >= this.contentTop() && y <= this.y1) {
            boolean hover = mouseX >= this.x0 + 4 && mouseX < this.x1 - 8 && mouseY >= y && mouseY < y + 20 && mouseY >= this.contentTop() && mouseY < this.y1;
            if (hover) {
               hoveredRow = row;
            }

            g.method_25294(this.x0 + 4, y + 1, this.x1 - 8, y + 20 - 1, hover ? -14405546 : -15064506);
            g.method_51427(row.icon(), this.x0 + 6, y + 2);
            g.method_44379(this.x0 + 24, y, this.segmentsX() - 4, y + 20);
            g.method_27535(this.font, row.displayName(), this.x0 + 26, y + 6, -1);
            g.method_44380();
            this.drawSegments(g, claim, row, y, editable, mouseX, mouseY);
         }

         y += 20;
      }

      this.hoveredPermTooltip = hoveredRow;
   }

   public List<class_2561> tooltip() {
      if (this.subTab == ClaimDetailPanel.SubTab.PERMS && this.hoveredPermTooltip != null) {
         List<class_2561> lines = new ArrayList();
         lines.add(this.hoveredPermTooltip.displayName());
         if (!this.hoveredPermTooltip.description().getString().isBlank()) {
            lines.add(this.hoveredPermTooltip.description());
         }

         lines.add(class_2561.method_43470("Default: ").method_27692(class_124.field_1080).method_10852(class_2561.method_43470(this.roleLetterName(this.hoveredPermTooltip.defaultRole())).method_27692(class_124.field_1068)));
         if (this.permSubId != null) {
            lines.add(class_2561.method_43470("I = inherit from parent").method_27692(class_124.field_1063));
         }

         return lines;
      } else {
         return null;
      }
   }

   private int drawSubChip(class_332 g, int chipX, int y, String label, boolean active, boolean hover) {
      int w = this.chipWidth(label);
      g.method_25294(chipX, y, chipX + w, y + 12, active ? -14405546 : -15064506);
      if (active) {
         g.method_49601(chipX, y, w, 12, -6467875);
      }

      g.method_25303(this.font, label, chipX + 4, y + 2, !active && !hover ? -7035976 : -1);
      return chipX + w + 4;
   }

   private int chipWidth(String label) {
      return this.font.method_1727(label) + 8;
   }

   private String chipAt(List<SubClaimEntry> subs, double mouseX) {
      int chipX = this.x0 + 6;
      int w = this.chipWidth("Parent");
      if (mouseX >= (double)chipX && mouseX < (double)(chipX + w)) {
         return "";
      } else {
         chipX += w + 4;

         for(SubClaimEntry sub : subs) {
            if (sub != null) {
               w = this.chipWidth(sub.getName());
               if (mouseX >= (double)chipX && mouseX < (double)(chipX + w)) {
                  return sub.getSubId();
               }

               chipX += w + 4;
            }
         }

         return null;
      }
   }

   private int segmentsX() {
      int count = this.permSubId != null ? 4 : 3;
      return this.x1 - 10 - count * 16;
   }

   private void drawSegments(class_332 g, ClaimDetailEntry claim, ClaimsState.PermRow row, int y, boolean editable, int mouseX, int mouseY) {
      String current = this.state.effectivePermRole(claim, this.permSubId, row.name());
      String rowRole = this.effectiveRole(claim, row.name());
      List<String> options = this.segmentOptions();
      int sx = this.segmentsX();

      for(String option : options) {
         boolean inherit = option.isEmpty();
         boolean selected = inherit ? this.permSubId != null && current == null : option.equals(current);
         boolean assignable = (inherit || row.assignableRoles().contains(option)) && this.canAssignRole(claim, rowRole, inherit ? null : option);
         boolean hover = editable && assignable && mouseX >= sx && mouseX < sx + 16 && mouseY >= y + 2 && mouseY < y + 20 - 2;
         int bg = selected ? this.segmentColor(option) : (hover ? -14405546 : -15657426);
         g.method_25294(sx, y + 3, sx + 16 - 1, y + 20 - 3, bg);
         String letter = inherit ? "I" : option.substring(0, 1);
         int color = !assignable ? -12959392 : (selected ? -16052186 : -7035976);
         g.method_25303(this.font, letter, sx + (16 - this.font.method_1727(letter)) / 2, y + 6, color);
         sx += 16;
      }

   }

   private List<String> segmentOptions() {
      List<String> options = new ArrayList();
      if (this.permSubId != null) {
         options.add("");
      }

      options.addAll(this.state.roles);
      return options;
   }

   private int segmentIndexAt(double mouseX, int optionCount) {
      if (mouseX < (double)this.segmentsX()) {
         return -1;
      } else {
         int index = (int)((mouseX - (double)this.segmentsX()) / (double)16.0F);
         return index < optionCount ? index : -1;
      }
   }

   private int segmentColor(String role) {
      int var10000;
      switch (role) {
         case "VISITOR" -> var10000 = -12474273;
         case "TRUSTED" -> var10000 = -2054081;
         case "OWNER" -> var10000 = -2734768;
         default -> var10000 = -6467875;
      }

      return var10000;
   }

   private String roleLetterName(String role) {
      return role == null ? "Inherit" : role.charAt(0) + role.substring(1).toLowerCase(Locale.ROOT);
   }

   private boolean inUntrustHitBox(double mouseX, double mouseY, int rowY) {
      return mouseX >= (double)(this.x1 - 30) && mouseX < (double)(this.x1 - 8) && mouseY >= (double)(rowY + 2) && mouseY < (double)(rowY + 20 - 2);
   }

   private void renderMembers(class_332 g, ClaimDetailEntry claim, int mouseX, int mouseY) {
      boolean canManage = this.canManageMembers(claim);
      int listTop = this.memberListTop(claim);
      int y = listTop - this.scroll;

      for(MemberEntry member : claim.getMembers() != null ? claim.getMembers() : List.<MemberEntry>of()) {
         if (member != null && member.getUuid() != null) {
            boolean removed = this.state.isOptimisticallyUntrusted(member.getUuid());
            if (y + 20 >= listTop && y <= this.y1) {
               g.method_25294(this.x0 + 4, y + 1, this.x1 - 8, y + 20 - 1, -15064506);
               int dot = member.getOnline() ? -12474273 : -10853256;
               g.method_25294(this.x0 + 8, y + 7, this.x0 + 13, y + 12, dot);
               class_2561 name = class_2561.method_43470(member.getName() == null ? "?" : member.getName()).method_27692(removed ? class_124.field_1055 : class_124.field_1068);
               g.method_27535(this.font, name, this.x0 + 18, y + 6, removed ? -7035976 : -1);
               if (member.isOwner()) {
                  String tag = "Owner";
                  g.method_25303(this.font, tag, this.x1 - 12 - this.font.method_1727(tag), y + 6, -2053377);
               } else {
                  if (member.getJoinedAt() > 0L) {
                     String joined = DATE_SHORT.format(new Date(member.getJoinedAt()));
                     g.method_25303(this.font, joined, this.segmentsX() - this.font.method_1727(joined), y + 6, -7035976);
                  }

                  if (canManage && !removed) {
                     boolean hover = this.inUntrustHitBox((double)mouseX, (double)mouseY, y);
                     g.method_25303(this.font, "✕", this.x1 - 12 - this.font.method_1727("✕"), y + 6, hover ? -1 : -7035976);
                  }
               }
            }

            y += 20;
         }
      }

      String pendingTrust = this.state.pendingTrustName(claim.getClaimId());
      if (pendingTrust != null) {
         if (y + 20 >= listTop && y <= this.y1) {
            g.method_25294(this.x0 + 4, y + 1, this.x1 - 8, y + 20 - 1, -15064506);
            g.method_27535(this.font, class_2561.method_43470(pendingTrust + " (pending)").method_27692(class_124.field_1056), this.x0 + 18, y + 6, -7035976);
         }

         y += 20;
      }

      List<MemberEntry> banned = canManage && claim.getBanned() != null ? claim.getBanned() : List.of();
      if (!banned.isEmpty()) {
         if (y + 20 >= listTop && y <= this.y1) {
            g.method_25303(this.font, "Banned", this.x0 + 8, y + 6, -2734768);
         }

         y += 20;

         for(MemberEntry entry : banned) {
            if (entry != null && entry.getUuid() != null) {
               if (y + 20 >= listTop && y <= this.y1) {
                  g.method_25294(this.x0 + 4, y + 1, this.x1 - 8, y + 20 - 1, -15064506);
                  g.method_25294(this.x0 + 8, y + 7, this.x0 + 13, y + 12, -2734768);
                  g.method_27535(this.font, class_2561.method_43470(entry.getName() == null ? "?" : entry.getName()), this.x0 + 18, y + 6, -7035976);
                  if (canManage) {
                     boolean hover = this.inUntrustHitBox((double)mouseX, (double)mouseY, y);
                     g.method_25303(this.font, "✕", this.x1 - 12 - this.font.method_1727("✕"), y + 6, hover ? -1 : -7035976);
                  }
               }

               y += 20;
            }
         }
      }

   }

   private void renderMessages(class_332 g, ClaimDetailEntry claim) {
      int x = this.x0 + 6;
      int y = this.contentTop() - this.scroll;
      boolean viewOnly = this.messagesReadOnly(claim);
      if (viewOnly) {
         g.method_27535(this.font, class_2561.method_43470("View Only").method_27692(class_124.field_1065), x, y + 2, -1);
         y += 16;
      }

      this.drawMessageLabel(g, x, y + 2, "Enter Title", this.enterTitleText);
      this.drawMessageLabel(g, x, y + 34, "Enter Subtitle", this.enterSubtitleText);
      this.drawMessageLabel(g, x, y + 66, "Leave Title", this.leaveTitleText);
      this.drawMessageLabel(g, x, y + 98, "Leave Subtitle", this.leaveSubtitleText);
   }

   private void drawMessageLabel(class_332 g, int x, int y, String label, String value) {
      g.method_27535(this.font, class_2561.method_43470(label).method_27692(class_124.field_1080), x, y, -1);
      int var10000 = value == null ? 0 : value.length();
      String counter = var10000 + "/" + this.state.maxMessageLength;
      g.method_25303(this.font, counter, this.x1 - 6 - this.font.method_1727(counter), y, -7035976);
   }

   private int contentHeight(ClaimDetailEntry claim) {
      int var10000;
      switch (this.subTab.ordinal()) {
         case 0 -> var10000 = this.infoLayout(claim).height();
         case 1 -> var10000 = 14 + this.state.permissionCatalog.size() * 20 + (claim.getSubClaims() != null && !claim.getSubClaims().isEmpty() ? 16 : 0) + 4;
         case 2 -> var10000 = ((claim.getMembers() != null ? claim.getMembers().size() + 1 : 1) + (claim.getBanned() != null && !claim.getBanned().isEmpty() ? claim.getBanned().size() + 1 : 0)) * 20 + 4;
         case 3 -> var10000 = 160 + (this.messagesReadOnly(claim) ? 16 : 0);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public int desiredHeight() {
      ClaimDetailEntry claim = this.claim();
      return claim == null ? 0 : this.contentTop() - this.y0 + this.contentHeight(claim) + 6;
   }

   private int maxScroll(ClaimDetailEntry claim) {
      return Math.max(0, this.contentHeight(claim) - (this.y1 - this.clipTop(claim) - 4));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      ClaimDetailEntry claim = this.claim();
      if (claim != null && button == 0) {
         if (mouseX >= (double)(this.x1 - 14) && mouseX < (double)(this.x1 - 2) && mouseY >= (double)(this.y0 + 2) && mouseY < (double)(this.y0 + 14)) {
            this.claimId = null;
            PreviewUi.playClick();
            return true;
         } else if (mouseY >= (double)this.headerBottom() && mouseY < (double)this.tabsBottom()) {
            SubTab clicked = this.subTabAt(mouseX);
            if (clicked != null && clicked != this.subTab) {
               this.subTab = clicked;
               this.scroll = 0;
               PreviewUi.playClick();
            }

            return true;
         } else if (!(mouseY < (double)this.contentTop()) && !(mouseY >= (double)this.y1)) {
            if (this.subTab == ClaimDetailPanel.SubTab.INFO) {
               return this.infoClicked(claim, mouseX, mouseY);
            } else if (this.subTab == ClaimDetailPanel.SubTab.PERMS) {
               return this.permsClicked(claim, mouseX, mouseY);
            } else {
               return this.subTab == ClaimDetailPanel.SubTab.MEMBERS ? this.membersClicked(claim, mouseX, mouseY) : false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean infoClicked(ClaimDetailEntry claim, double mouseX, double mouseY) {
      InfoLayout layout = this.infoLayout(claim);
      if (claim.isOwner() && layout.subChipsY() >= 0) {
         boolean[] handled = new boolean[]{false};
         this.forEachSubChip(claim, this.contentTop() - this.scroll + layout.subChipsY(), (sub, chipX, chipY, w) -> {
            if (!(mouseX < (double)chipX) && !(mouseX >= (double)(chipX + w)) && !(mouseY < (double)chipY) && !(mouseY >= (double)(chipY + 12))) {
               this.infoSubId = sub.getSubId();
               this.syncEditBoxValues();
               PreviewUi.playClick();
               handled[0] = true;
               return false;
            } else {
               return true;
            }
         });
         return handled[0];
      } else {
         return false;
      }
   }

   private class_2561 permsLegend() {
      class_5250 legend = class_2561.method_43473();
      boolean first = true;

      for(String role : this.state.roles) {
         if (!first) {
            legend.method_10852(class_2561.method_43470("  "));
         }

         first = false;
         legend.method_10852(class_2561.method_43470(role.substring(0, 1) + " ").method_27694((style) -> style.method_27703(class_5251.method_27717(this.segmentColor(role) & 16777215))));
         legend.method_10852(class_2561.method_43470(this.roleLetterName(role)).method_27692(class_124.field_1080));
      }

      return legend;
   }

   private boolean permsClicked(ClaimDetailEntry claim, double mouseX, double mouseY) {
      int y = this.contentTop() - this.scroll + 14;
      List<SubClaimEntry> subs = claim.getSubClaims() != null ? claim.getSubClaims() : List.of();
      if (!subs.isEmpty()) {
         if (mouseY >= (double)y && mouseY < (double)(y + 12)) {
            String chip = this.chipAt(subs, mouseX);
            if (chip != null) {
               this.permSubId = "".equals(chip) ? null : chip;
               PreviewUi.playClick();
            }

            return chip != null;
         }

         y += 16;
      }

      if (!this.canEditPerms(claim)) {
         int rowsY = y;

         for(ClaimsState.PermRow ignored : this.state.permissionCatalog) {
            if (mouseY >= (double)(rowsY + 2) && mouseY < (double)(rowsY + 20 - 2) && this.segmentIndexAt(mouseX, this.segmentOptions().size()) >= 0) {
               this.state.showBanner(this.state.screenText("deny_edit_permissions"), true);
               return true;
            }

            rowsY += 20;
         }

         return false;
      } else {
         for(ClaimsState.PermRow row : this.state.permissionCatalog) {
            if (mouseY >= (double)(y + 2) && mouseY < (double)(y + 20 - 2)) {
               List<String> options = this.segmentOptions();
               int index = this.segmentIndexAt(mouseX, options.size());
               if (index < 0) {
                  return false;
               }

               String option = (String)options.get(index);
               boolean inherit = option.isEmpty();
               if (!inherit && !row.assignableRoles().contains(option)) {
                  return true;
               }

               if (inherit && this.permSubId == null) {
                  return true;
               }

               String role = inherit ? null : option;
               if (!this.canAssignRole(claim, this.effectiveRole(claim, row.name()), role)) {
                  this.state.showBanner(this.state.screenText("deny_restrict_permission"), true);
                  return true;
               }

               this.state.optimisticSetPerm(claim.getClaimId(), this.permSubId, row.name(), role);
               ClaimsNetworking.sendSetPermission(claim.getClaimId(), this.permSubId, row.name(), role);
               PreviewUi.playClick();
               return true;
            }

            y += 20;
         }

         return false;
      }
   }

   private boolean membersClicked(ClaimDetailEntry claim, double mouseX, double mouseY) {
      if (!this.canManageMembers(claim)) {
         return false;
      } else if (mouseY < (double)this.memberListTop(claim)) {
         return false;
      } else {
         int y = this.memberListTop(claim) - this.scroll;

         for(MemberEntry member : claim.getMembers() != null ? claim.getMembers() : List.<MemberEntry>of()) {
            if (member != null && member.getUuid() != null) {
               if (this.inUntrustHitBox(mouseX, mouseY, y)) {
                  if (!member.isOwner() && !this.state.isOptimisticallyUntrusted(member.getUuid())) {
                     String claimId = claim.getClaimId();
                     String uuid = member.getUuid();
                     String name = member.getName();
                     this.confirm.request(this.state.screenText("confirm_remove_member_title"), this.state.screenText("confirm_remove_member_body", "player", name == null ? "this member" : name), () -> {
                        this.state.optimisticUntrust(uuid);
                        ClaimsNetworking.sendUntrust(claimId, uuid, name);
                     });
                     return true;
                  }

                  return false;
               }

               y += 20;
            }
         }

         if (this.state.pendingTrustName(claim.getClaimId()) != null) {
            y += 20;
         }

         List<MemberEntry> banned = claim.getBanned() != null ? claim.getBanned() : List.of();
         if (banned.isEmpty()) {
            return false;
         } else {
            y += 20;

            for(MemberEntry entry : banned) {
               if (entry != null && entry.getUuid() != null) {
                  if (this.inUntrustHitBox(mouseX, mouseY, y)) {
                     String claimId = claim.getClaimId();
                     String uuid = entry.getUuid();
                     String name = entry.getName();
                     this.confirm.request(this.state.screenText("confirm_unban_title"), this.state.screenText("confirm_unban_body", "player", name == null ? "this player" : name), () -> ClaimsNetworking.sendUnban(claimId, uuid, name));
                     return true;
                  }

                  y += 20;
               }
            }

            return false;
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      ClaimDetailEntry claim = this.claim();
      if (claim != null && this.contains(mouseX, mouseY)) {
         this.scroll = class_3532.method_15340(this.scroll - (int)(scrollY * (double)20.0F), 0, this.maxScroll(claim));
         return true;
      } else {
         return false;
      }
   }

   static {
      DATE_MEDIUM = DateFormat.getDateInstance(2, Locale.ENGLISH);
      DATE_SHORT = DateFormat.getDateInstance(3, Locale.ENGLISH);
   }

   @Environment(EnvType.CLIENT)
   public static enum SubTab {
      INFO,
      PERMS,
      MEMBERS,
      MESSAGES;

      // $FF: synthetic method
      private static SubTab[] $values() {
         return new SubTab[]{INFO, PERMS, MEMBERS, MESSAGES};
      }
   }

   @Environment(EnvType.CLIENT)
   private static record InfoLayout(int nameLabelY, int nameRowY, int buttonRowY, int transferLabelY, int transferRowY, int subsLabelY, int subChipsY, int subNameRowY, int subButtonRowY, int height) {
      static final InfoLayout EMPTY = new InfoLayout(-1, -1, -1, -1, -1, -1, -1, -1, -1, 0);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface ConfirmRequest {
      void request(class_2561 var1, class_2561 var2, Runnable var3);
   }

   @Environment(EnvType.CLIENT)
   private interface SubChipVisitor {
      boolean visit(SubClaimEntry var1, int var2, int var3, int var4);
   }
}
