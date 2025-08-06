<#macro emailLayout>
<body>
    <div class="logo">
        <img src="https://raw.githubusercontent.com/lvoxx/SRMS-backend/refs/heads/main/Images/SRMS-Logo.png" alt="SRMS Logo" width="150" />
    </div>
    <div class="content">
        <#nested>
        <div>
            <p style="font-size: 24px; color: #919191FF;">${kcSanitize(msg("systemMessage"))?no_esc}</p>
        </div>
    </div>  
</body>
</html>
</#macro>