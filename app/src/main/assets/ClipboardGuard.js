/**
 * @name: ClipboardGuard
 * @author: huanchengfly, 0Ranko0p
 **/
!function(){"use strict";document.addEventListener("copy",(function(t){confirm("ClipboardGuardCopyRequest")||(t.preventDefault(),t.stopPropagation())}),{passive:!1,capture:!0})}();